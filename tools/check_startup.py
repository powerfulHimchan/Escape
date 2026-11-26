"""Check the actual journal, shutter scene and inventory on Android 17."""
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
            if node.get("text") == label:
                return tuple(map(int, re.findall(r"\d+", node.attrib["bounds"])))
        time.sleep(.4)
    raise AssertionError(label)


def tap(label):
    x1, y1, x2, y2 = find(label)
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))
    time.sleep(1)


def capture(name):
    Path("Escape-" + name + ".png").write_bytes(adb("exec-out", "screencap", "-p"))


adb("install", "-r", "AfterLastTrain-0.2.0.apk")
adb("shell", "am", "start", "-W", "-n", "com.powerfulhimchan.escape/.MainActivity")
find("이야기 시작")
capture("Title")
tap("이야기 시작")
find("노선도에 없는 역")
capture("Platform")
tap("계속")
tap("벽의 칸 번호를 기억한다")
find("시간 제한 선택 열기")
capture("Shutter")
tap("소지품")
find("아직 소지품이 없다.")
capture("Inventory")
adb("shell", "pidof", "com.powerfulhimchan.escape")
