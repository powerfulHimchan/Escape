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
    assert len(chapter["links"]) >= 2
    assert all(0 < int(step) < len(beats) and text.strip()
               for step, text in chapter["links"].items())
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

for c in story["chapters"]:
    for step, view in c.get("linkViews", {}).items():
        assert step in c["links"] and view["title"] and 0 <= view["setting"] < 12
    tag_entries = [b for b in c["beats"] if b.get("mode") == "tagReview"]
    assert len(tag_entries) == (1 if c["act"] >= 3 else 0)
    assert sum("마지막 빈칸" in b["body"] for b in c["beats"]) == 0
    if c["act"] == 2:
        entry = next(b for b in c["beats"] if b["title"] == "종이에 남는 것")
        assert "tagSketch" in entry["choices"][0]["effect"] and "tagTime" in entry["choices"][1]["effect"]
        assert c["linkViews"]["2"]["setting"] == 0
train = next(c for c in story["chapters"] if c["key"] == "train")
assert all(b["setting"] != 6 for b in train["beats"])
