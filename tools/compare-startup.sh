#!/usr/bin/env bash
set -euo pipefail

adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
apk_package=com.powerfulhimchan.escape
adb install Escape-original.apk
adb logcat -c
adb shell am start -W -n "$apk_package/.MainActivity" || true
sleep 3
adb logcat -d -b crash -v brief | tee Escape-original-crash.txt
adb shell dumpsys activity activities | grep -E 'ResumedActivity|topResumedActivity' | tee Escape-original-active.txt || true
adb shell pidof "$apk_package" || true
adb uninstall "$apk_package"
adb install Escape-corrected.apk
adb logcat -c
adb shell am start -W -n "$apk_package/.MainActivity"
sleep 3
adb logcat -d -b crash -v brief | tee Escape-corrected-crash.txt
adb shell dumpsys activity activities | grep -E 'ResumedActivity|topResumedActivity' | tee Escape-corrected-active.txt
adb shell pidof "$apk_package"
grep -F "$apk_package/.MainActivity" Escape-corrected-active.txt
if grep -F "Process: $apk_package" Escape-corrected-crash.txt; then exit 1; fi
adb shell uiautomator dump /sdcard/escape-title.xml
adb pull /sdcard/escape-title.xml Escape-title.xml
python3 -c 'import re,sys,xml.etree.ElementTree as E; n=next(n for n in E.parse("Escape-title.xml").iter("node") if n.get("text")=="이야기 시작"); x1,y1,x2,y2=map(int,re.findall(r"\d+",n.attrib["bounds"])); print((x1+x2)//2,(y1+y2)//2)' > Escape-start-button.txt
read -r x y < Escape-start-button.txt
adb shell input tap "$x" "$y"
sleep 2
adb shell uiautomator dump /sdcard/escape-intro.xml
adb pull /sdcard/escape-intro.xml Escape-intro.xml
grep -F '노선도에 없는 역' Escape-intro.xml >/dev/null
adb shell pidof "$apk_package"
adb logcat -d -b crash -v brief | tee Escape-corrected-crash.txt
if grep -F "Process: $apk_package" Escape-corrected-crash.txt; then exit 1; fi
echo "Title and story opening reached successfully."
