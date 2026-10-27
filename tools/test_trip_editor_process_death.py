"""Local debug fixture QA. Requires an unlocked tablet and an installed debug APK.

Only clears the dedicated fixture task and kills com.havamania:editor_test.
Does not clear app data, force-stop the package, or save a real trip.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    args = parser.parse_args()
    component = "com.havamania/.TravelEditorProcessDeathActivity"

    def adb(*command):
        return subprocess.run([args.adb, *command], check=True, capture_output=True,
                              text=True, encoding="utf-8", timeout=30).stdout

    def nodes():
        adb("shell", "uiautomator", "dump", "/sdcard/trip-editor-process-qa.xml")
        raw = adb("exec-out", "cat", "/sdcard/trip-editor-process-qa.xml")
        return list(ET.fromstring(raw).iter("node"))

    def wait_for(predicate):
        deadline = time.monotonic() + 20
        while time.monotonic() < deadline:
            found = next((node for node in nodes() if predicate(node)), None)
            if found is not None:
                return found
            time.sleep(0.3)
        raise AssertionError("Expected fixture control was not visible")

    def tap(node):
        bounds = list(map(int, re.findall(r"\d+", node.attrib["bounds"])))
        adb("shell", "input", "tap", str((bounds[0] + bounds[2]) // 2),
            str((bounds[1] + bounds[3]) // 2))

    # NEW_TASK | CLEAR_TASK: the fixture has its own task affinity.
    output = adb("shell", "am", "start", "-W", "-n", component, "-f", "0x10008000")
    if "Error" in output or "Exception" in output:
        raise AssertionError(output)
    tap(wait_for(lambda n: n.get("text") == "Test düzenleyicisini aç"))
    tap(wait_for(lambda n: n.get("class") == "android.widget.EditText"))
    draft = "ProcessDeathDraft"
    adb("shell", "input", "text", draft)
    wait_for(lambda n: n.get("text") == draft)
    process_id = adb("shell", "pidof", "com.havamania:editor_test").strip()
    if not process_id.isdecimal():
        raise AssertionError("Expected exactly one fixture process")
    adb("shell", "input", "keyevent", "KEYCODE_HOME")
    # Allow onStop/onSaveInstanceState to complete before killing this process.
    time.sleep(2)
    if adb("shell", "pidof", "com.havamania:editor_test").strip() != process_id:
        raise AssertionError("Fixture process changed before kill")
    adb("shell", "run-as", "com.havamania", "/system/bin/toybox", "kill", process_id)
    time.sleep(0.5)
    # pidof exits 1 for no process; inspect without check=True.
    stopped = subprocess.run([args.adb, "shell", "pidof", "com.havamania:editor_test"],
                             capture_output=True, text=True, timeout=10)
    if stopped.stdout.strip():
        raise AssertionError("Fixture process did not stop")
    # NEW_TASK | RESET_TASK_IF_NEEDED resumes the retained fixture task.
    adb("shell", "am", "start", "-W", "-n", component, "-f", "0x10200000")
    wait_for(lambda n: n.get("class") == "android.widget.EditText" and n.get("text") == draft)
    new_id = adb("shell", "pidof", "com.havamania:editor_test").strip()
    if not new_id.isdecimal() or new_id == process_id:
        raise AssertionError("Expected a new fixture process")
    log = adb("logcat", "-d", "-s", "TripEditorProcessQA:I", "*:S")
    if f"pid={new_id} restored=true" not in log:
        raise AssertionError("New Activity did not report restored instance state")
    print(json.dumps({"passed": True, "old_pid": process_id, "new_pid": new_id,
                      "draft_restored": True, "saved_instance_state": True}))


if __name__ == "__main__":
    main()
