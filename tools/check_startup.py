"""Check the actual journal, shutter scene and inventory on Android 17."""
import json
import re
import struct
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def adb(*args):
    return subprocess.check_output(["adb", *args])


def find(label):
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/escape.xml")
        tree = ET.fromstring(adb("shell", "cat", "/sdcard/escape.xml"))
        for node in tree.iter("node"):
            if label in (node.get("text"), node.get("content-desc")):
                return tuple(map(int, re.findall(r"\d+", node.attrib["bounds"])))
        time.sleep(.4)
    raise AssertionError(label)


def tap(label):
    x1, y1, x2, y2 = find(label)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1)


def capture(name):
    Path("Escape-" + name + ".png").write_bytes(adb("exec-out", "screencap", "-p"))


PACKAGE = "com.powerfulhimchan.escape"


def saved_state():
    prefs = ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/escape.xml"))
    return json.loads(prefs.find("string[@name='story.save']").text)["state"]


def await_state(predicate):
    deadline = time.monotonic() + 17
    state = None
    while time.monotonic() < deadline:
        # SharedPreferences.apply writes asynchronously and renames the XML
        # during replacement. The UI can advance before the file is readable.
        try:
            state = saved_state()
        except (subprocess.CalledProcessError, ET.ParseError):
            time.sleep(.1)
            continue
        if predicate(state):
            return state
        time.sleep(.1)
    raise AssertionError(state)


def art_pixels():
    raw = adb("exec-out", "screencap")
    width, height, pixel_format = struct.unpack_from("<III", raw)
    assert pixel_format in (1, 2), pixel_format
    data = memoryview(raw)[-width * height * 4:]
    return b"".join(data[(y * width + width // 5) * 4:(y * width + width * 4 // 5) * 4]
                    for y in range(height // 5, height * 3 // 10, 2))


def reach_shutter(right_path=False, inspect_train=False):
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    find("이야기 시작")
    capture("Title")
    tap("이야기 시작")
    if inspect_train:
        departing = art_pixels()
        capture("Departing")
        time.sleep(3.8)
        assert art_pixels() != departing, "Station did not change as the train left"
    find("노선도에 없는 역")
    if inspect_train:
        capture("Platform")
    tap("계속")
    # Both scenes use the same two footer buttons. Locate the upper button
    # before the clock starts so a slow accessibility dump cannot consume it.
    x1, y1, x2, y2 = find("왼쪽 개찰구를 지나 손자국을 따라간다")
    upper = (str((x1 + x2) // 2), str((y1 + y2) // 2))
    capture("Gates")
    if right_path:
        global lower_button
        a, b, c, d = find("오른쪽 개찰구를 지나 정차 기록을 확인한다")
        lower_button = (str((a+c)//2), str((b+d)//2))
        adb("shell", "input", "tap", *lower_button)
    else:
        adb("shell", "input", "tap", *upper)
    arrived = await_state(lambda state: state["step"] == 1 and state["page"] == "link")
    assert "timeLeft" not in arrived
    assert arrived["flags"].get("zeroCount" if right_path else "handprint")
    a, b, c, d = find("계속")
    global shutter_start
    shutter_start = time.monotonic()
    adb("shell", "input", "tap", str((a+c)//2), str((b+d)//2))
    await_state(lambda state: state["step"] == 1 and "timeLeft" in state)
    return upper


adb("install", "-r", "AfterLastTrain-0.6.0.apk")
upper = reach_shutter(inspect_train=True)
time.sleep(.35)
capture("Shutter")
adb("shell", "input", "tap", *upper)
time.sleep(.55)
capture("Pickup")
chosen = await_state(lambda state: state["step"] == 2)
assert chosen["flags"].get("radio") and len(chosen["log"]) == 2
find("계속")
tap("소지품")
tap("무전기")
find("사용하려면 건전지가 필요하다.")
capture("Inventory")

# Use a fresh run to verify the unattended result on the same timed scene.
adb("shell", "pm", "clear", PACKAGE)
reach_shutter(right_path=True)
early = art_pixels()
time.sleep(3)
assert art_pixels() != early, "Shutter did not move inside the scene"
capture("Closing")
expired = await_state(lambda state: state["step"] == 2)
assert expired["flags"].get("detour") and not expired["flags"].get("radio")
assert len(expired["log"]) == 2 and expired["log"][-1]["label"] == "시간 안에 고르지 못함"
adb("shell", "pm", "clear", PACKAGE)
reach_shutter()
# Both choice footers are anchored at the bottom. Read the lower button
# on the untimed gate scene in the earlier run, then tap it during the timer.
time.sleep(max(0, shutter_start + 7.25 - time.monotonic()))
adb("shell", "input", "tap", *lower_button)
time.sleep(.78)
capture("Run")
escaped = await_state(lambda state: state["step"] == 2)
assert escaped["flags"].get("ran") and not escaped["flags"].get("radio")
adb("shell", "pidof", PACKAGE)

time.sleep(1.5)
find("계속")
capture("UpperKiosk")

# Start at the old staircase fork through a regular saved game. Both choices
# must play on the same landing and arrive at different chapter openings.
def resume_at(state):
    adb("shell", "am", "force-stop", PACKAGE)
    prefs = ET.Element("map")
    ET.SubElement(prefs, "string", name="story.save").text = json.dumps(
        {"state": state, "checkpoints": [None, None, None, state]})
    subprocess.run(["adb", "shell", "run-as", PACKAGE, "tee", "shared_prefs/escape.xml"],
                   input=ET.tostring(prefs), stdout=subprocess.DEVNULL, check=True)
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    tap("이어하기")



def staircase_fork(button, expected, screenshot):
    resume_at({"act": 4, "branch": 7, "step": 11, "page": "choice", "flags": {}, "log": []})
    x1, y1, x2, y2 = find(button)
    before = art_pixels()
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1.25)
    capture(screenshot)
    chosen = await_state(lambda s: s["act"] == 5)
    assert chosen["branch"] == expected and chosen["departedView"] == 35
    find("계속")
    assert art_pixels() != before, "Arrival reused the departure scene"
    capture(screenshot + "Arrival")
    return art_pixels()


upper_landing = staircase_fork("계단 끝까지 올라가 직접 확인한다", 14, "Upstairs")
lower_landing = staircase_fork("현재 통로와 안내도를 대조해 돌아간다", 15, "Downstairs")
assert upper_landing != lower_landing, "Both stair exits showed the same destination"

resume_at({"act": 2, "branch": 1, "step": 0, "page": "intro", "flags": {}, "log": []})
find("대합실")
capture("HallScale")


def at(act, branch, step, page="choice", flags=None, log=None):
    resume_at({"act": act, "branch": branch, "step": step, "page": page,
               "flags": flags or {}, "log": log or []})


def visible_text():
    adb("shell", "uiautomator", "dump", "/sdcard/escape.xml")
    tree = ET.fromstring(adb("shell", "cat", "/sdcard/escape.xml"))
    return " ".join(node.get("text", "") for node in tree.iter("node"))


at(2, 0, 1)
tap("안내도를 들고 간다")
find("처음 내렸던 승강장")
assert "안주머니" in visible_text()
capture("PlatformReset")
tap("계속")
find("컵에 남은 물")
capture("OfficeCup")
tap("수위를 찍고 물을 조금 덜어 둔다")
assert await_state(lambda s: s["step"] == 3)["flags"]["cupMeasured"]
at(2, 0, 9, flags={"cupMeasured": True})
assert "사진 속 손잡이" in visible_text()
at(2, 0, 9)
assert "사진 속 손잡이" not in visible_text()

# Read a 0.5 save with only its original choice label to recover the surviving note.
at(3, 0, 4, log=[{"act": 2, "label": "표찰 한 칸에 정차 시각을 쓴다"}])
assert "표찰에 적어 둔 00:11" in visible_text()

def ending_cast(branch, button, ending_id, minseo, jian):
    at(5, branch, 8)
    tap(button)
    state = await_state(lambda s: s["page"] == "ending")
    assert state["ending"] == ending_id
    prefs = ET.fromstring(adb("shell", "run-as", PACKAGE, "cat", "shared_prefs/escape.xml"))
    archives = [json.loads(n.text) for n in prefs.findall("string") if n.get("name") != "story.save"]
    record = next(a[ending_id][-1] for a in archives if ending_id in a)
    assert (record["withMinseo"], record["withJian"]) == (minseo, jian)

ending_cast(3, "둘을 방송실 쪽으로 안내한다", "broadcast", True, True)
ending_cast(13, "민서와 정차 기록을 갖고 함께 나간다", "receipt", True, False)
ending_cast(13, "지금 가진 자료만 들고 먼저 나간다", "receipt", False, False)
