"""Check the actual journal, shutter scene and inventory on Android 17."""
import json
import re
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
    while time.monotonic() < deadline:
        state = saved_state()
        if predicate(state):
            return state
        time.sleep(.1)
    raise AssertionError(state)


def reach_shutter():
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivity")
    find("이야기 시작")
    capture("Title")
    tap("이야기 시작")
    find("노선도에 없는 역")
    capture("Platform")
    tap("계속")
    # Both scenes use the same two footer buttons. Locate the upper button
    # before the clock starts so a slow accessibility dump cannot consume it.
    x1, y1, x2, y2 = find("벽의 칸 번호를 기억한다")
    upper = (str((x1 + x2) // 2), str((y1 + y2) // 2))
    adb("shell", "input", "tap", *upper)
    await_state(lambda state: state["step"] == 1 and "timeLeft" in state)
    return upper


adb("install", "-r", "AfterLastTrain-0.2.0.apk")
upper = reach_shutter()
time.sleep(.35)
capture("Shutter")
adb("shell", "input", "tap", *upper)
chosen = await_state(lambda state: state["step"] == 2)
assert chosen["flags"].get("radio") and len(chosen["log"]) == 2
time.sleep(1)
tap("소지품")
tap("무전기")
find("사용하려면 건전지가 필요하다.")
capture("Inventory")

# Use a fresh run to verify the unattended result on the same timed scene.
adb("shell", "pm", "clear", PACKAGE)
reach_shutter()
expired = await_state(lambda state: state["step"] == 2)
assert expired["flags"].get("detour") and not expired["flags"].get("radio")
assert len(expired["log"]) == 2 and expired["log"][-1]["label"] == "시간 안에 고르지 못함"
adb("shell", "pidof", PACKAGE)
