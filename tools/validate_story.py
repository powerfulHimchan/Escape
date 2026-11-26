"""Check all route lengths, timed choices, terminal outcomes, and inventory alternates."""
import json
from collections import Counter
from pathlib import Path

story = json.loads(Path("app/src/main/assets/story.json").read_text(encoding="utf-8"))
chapters = {(c["act"], c["index"]): c for c in story["chapters"]}
assert len(chapters) == 31
expected = (8, 10, 11, 12, 9)
targets = set()
routes = []


def visit(act, index, decisions=0, timed=0):
    chapter = chapters[act, index]
    beats = chapter["beats"]
    assert len(beats) == expected[act - 1], chapter["key"]
    assert all(chapter[key].strip() for key in ("opening", "sceneBreak", "name"))
    for beat in beats:
        assert all(beat[key].strip() for key in ("title", "body"))
        assert len(beat["choices"]) == 2
        assert len({choice["label"] for choice in beat["choices"]}) == 2
        assert all(choice["after"].strip() for choice in beat["choices"])
        if beat.get("seconds"):
            assert 6 <= beat["seconds"] <= 10 and beat["timeout"].strip()
    assert sum("seconds" in b for b in beats) == 1
    decisions += len(beats)
    timed += 1
    if act < 5:
        for option in range(2):
            visit(act + 1, 2 * index + option, decisions, timed)
    else:
        assert decisions == 50 and timed == 5
        assert len(chapter["ends"]) == 2
        routes.append(chapter["key"])
        targets.update(chapter["ends"])


visit(1, 0)
assert len(set(routes)) == 16
assert Counter(e["type"] for e in story["endings"]) == {
    "기존": 3, "배드": 2, "열린": 2, "해피": 2, "허무": 2
}
assert targets == {e["id"] for e in story["endings"]}
for ending in story["endings"]:
    assert len(ending["alt"]) >= 2
    assert ending["alt"][-1][0] == "default"
    assert len({variant[1] for variant in ending["alt"]}) == len(ending["alt"])
print(f"Verified {len(chapters)} chapters, {len(routes)} full routes, "
      f"{len(routes) * 2} terminal actions and {len(targets)} endings")
