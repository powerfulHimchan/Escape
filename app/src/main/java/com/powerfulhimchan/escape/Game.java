package com.powerfulhimchan.escape;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

final class Game {
    private static final String[] TURN_SIGNALS = {
            "radio", "poweredRadio", "poweredLamp", "map", "mark", "proofPlace",
            "proofTime", "promise", "trust", "code", "warned", "riderHelp", "signal", "detour"
    };

    JSONObject state;
    JSONArray checkpoints;
    private final Story story;

    Game(Story story, String saved) {
        this.story = story;
        try {
            JSONObject root = new JSONObject(saved);
            state = root.getJSONObject("state");
            checkpoints = root.getJSONArray("checkpoints");
        } catch (JSONException | RuntimeException invalidSave) {
            restart();
        }
    }

    void restart() {
        state = new JSONObject();
        checkpoints = new JSONArray();
        put(state, "act", 1);
        put(state, "branch", 0);
        put(state, "step", 0);
        put(state, "page", "intro");
        put(state, "flags", new JSONObject());
        put(state, "log", new JSONArray());
        checkpoint();
    }

    static void put(JSONObject object, String key, Object value) {
        try {
            object.put(key, value);
        } catch (JSONException error) {
            throw new IllegalStateException(key, error);
        }
    }

    private static JSONObject copy(JSONObject object) {
        try {
            return new JSONObject(object.toString());
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }
    }

    String serialize() {
        JSONObject root = new JSONObject();
        put(root, "state", state);
        put(root, "checkpoints", checkpoints);
        return root.toString();
    }

    int act() { return state.optInt("act", 1); }
    int step() { return state.optInt("step"); }
    String page() { return state.optString("page"); }
    JSONObject chapter() { return story.chapter(act(), state.optInt("branch")); }
    JSONObject beat() { return Story.beat(chapter(), step()); }
    String last() { return state.optString("last"); }
    boolean has(String key) { return state.optJSONObject("flags").optBoolean(key); }
    private boolean ownsBattery() { return has("battery") && !has("poweredRadio") && !has("poweredLamp"); }

    void continueScene() {
        if ("intro".equals(page()) || "link".equals(page())) {
            put(state, "page", "choice");
        }
    }

    private void checkpoint() {
        try {
            checkpoints.put(act() - 1, copy(state));
        } catch (JSONException error) {
            throw new IllegalStateException(error);
        }
    }

    boolean replay(int act) {
        JSONObject snapshot = checkpoints.optJSONObject(act - 1);
        if (snapshot == null) return false;
        state = copy(snapshot);
        for (int i = checkpoints.length() - 1; i >= act; i--) checkpoints.remove(i);
        return true;
    }

    String choiceLabel(int index) {
        JSONObject beat = beat();
        JSONObject option = Story.option(beat, index);
        if (index == 0 && "supply".equals(beat.optString("mode")) && has("radio"))
            return "건전지를 무전기에 넣고 호출을 받는다";
        if ("power".equals(beat.optString("mode"))) {
            if (index == 0 && (!has("radio") || !ownsBattery())) {
                return has("poweredRadio") ? "무전기의 신호를 확인한다" : "고정 호출기의 위치를 확인한다";
            }
            if (index == 1 && (!has("lamp") || !ownsBattery())) {
                return has("poweredLamp") ? "손전등으로 모퉁이를 비춘다" : "비상등 아래에서 방향을 기억한다";
            }
        }
        String label = option.optString("label");
        String route = chapter().optString("key");
        if (act() == 5 && step() == 8) {
            if (index == 0 && "change".equals(route) && has("poweredRadio")) {
                return "무전기로 바뀐 합류 지점을 전한다";
            }
            if (index == 0 && "rescueDark".equals(route) && has("poweredLamp")) {
                return "손전등으로 길을 비추며 함께 나간다";
            }
            if (index == 0 && "dawn".equals(route) && !has("poweredRadio") && (!has("radio") || !ownsBattery())) {
                return "정류장으로 나가 약속한 소리를 기다린다";
            }
        }
        return label;
    }

    JSONObject linkView() {
        JSONObject views=chapter().optJSONObject("linkViews");
        return "link".equals(page()) && views!=null ? views.optJSONObject(String.valueOf(step())) : null;
    }

    // Re-read revised outcomes for an older save paused on a transition.
    String linkResult() {
        JSONObject previous=Story.beat(chapter(),step()-1);
        JSONArray log=state.optJSONArray("log");
        JSONObject entry=log.optJSONObject(log.length()-1);
        if (entry==null) return "";
        String effects=entry.optString("effect");
        if (previous.has("seconds") && effects.equals(previous.optString("timeoutEffect")))
            return previous.optString("timeout");
        for (int index=0;index<2;index++) {
            JSONObject choice=Story.option(previous,index);
            if (choice.optString("label").equals(entry.optString("label")) ||
                    effects.equals(choice.optString("effect"))) return choice.optString("after");
        }
        return last();
    }

    String tagNote() {
        boolean sketch=has("tagSketch"), time=has("tagTime");
        if (!sketch && !time) {
            JSONArray log=state.optJSONArray("log");
            for (int i=0;i<log.length();i++) {
                JSONObject entry=log.optJSONObject(i);
                if (entry.optInt("act")!=2) continue;
                String label=entry.optString("label");
                if (label.equals("표찰 한 칸에 위치를 그린다")) sketch=true;
                if (label.equals("표찰 한 칸에 정차 시각을 쓴다")) time=true;
            }
        }
        if (sketch) return "표찰에는 승강장까지 꺾어 가는 길이 그려져 있다. 도윤은 종이를 돌려 자기가 걸어온 방향에 맞췄다.";
        if (time) return "표찰에 적어 둔 00:11은 그대로다. 도윤은 지금 들리는 소리가 그때로부터 얼마나 뒤인지 가늠했다.";
        return "";
    }

    String timerClue() {
        if (act() == 5 && step() == 8 && "streetProof".equals(chapter().optString("key"))
                && (!has("proofPlace") || !has("proofTime"))) {
            return "기록에 장소나 시각이 빠져 있다. 출구를 건너면 지금 가진 자료만 보여줄 수 있다.";
        }
        return "";
    }

    String select(int index, int view) {
        JSONObject ch = chapter();
        JSONObject beat = beat();
        boolean timeout = index < 0;
        int currentAct = act();
        boolean finalStep = step() == Story.LENGTHS[currentAct - 1] - 1;
        JSONObject option = timeout ? null : Story.option(beat, index);
        String label = timeout ? "시간 안에 고르지 못함" : choiceLabel(index);
        String aftermath = timeout ? beat.optString("timeout") : option.optString("after");
        String effects = timeout ? beat.optString("timeoutEffect") : option.optString("effect");
        JSONObject flags = state.optJSONObject("flags");
        boolean important = finalStep || timeout;
        String motion = "hold";
        if (index == 0 && finalStep && "dawn".equals(ch.optString("key"))
                && has("radio") && ownsBattery()) effects += ",poweredRadio";
        if (index == 0 && "supply".equals(beat.optString("mode")) && has("radio")) {
            effects = "battery,poweredRadio,signal";
            aftermath = "건전지를 밀어 넣자 무전기에서 구두 굽 소리와 짧은 숨소리가 들렸다. 벽 안내판은 켜지지 않았다.";
        }
        if ("powerRadio".equals(effects)) {
            if (has("radio") && ownsBattery()) {
                effects = "poweredRadio,signal";
            } else {
                effects = "signal";
                aftermath = has("poweredRadio") ? "작은 표시등이 깜박인다. 도윤은 같은 신호를 알아볼 수 있다."
                        : "고정 호출기의 위치를 기억해 두었다.";
            }
        } else if ("powerLamp".equals(effects)) {
            if (has("lamp") && ownsBattery()) {
                effects = "poweredLamp,darkRoute";
            } else {
                effects = "darkRoute";
                aftermath = has("poweredLamp") ? "켜고 끄는 불빛으로 벽의 꺾이는 부분을 확인했다."
                        : "비상등 아래에서 다음 모퉁이에 이르는 방향을 기억했다.";
            }
        }
        for (String name : effects.split(",")) {
            if (name.isEmpty()) continue;
            if ("run".equals(name) || "pick".equals(name)) {
                motion = "run".equals(name) ? (index == 1 ? "runRight" : "runLeft") : name;
                continue;
            }
            if (has(name)) continue;
            put(flags, name, true);
            for (String signal : TURN_SIGNALS) {
                if (signal.equals(name)) important = true;
            }
        }

        motion = option == null ? (currentAct == 1 ? "backstep" : "hold") : option.optString("motion", motion);
        boolean moved = StationArt.travels(motion) || view == 29 && motion.equals("pick")
                || view == 13 && motion.equals("pick");
        if (moved) put(state, "departedView", view);

        JSONObject entry = new JSONObject();
        put(entry, "act", currentAct);
        put(entry, "label", label);
        put(entry, "after", aftermath);
        put(entry, "important", important);
        put(entry, "fork", finalStep);
        put(entry, "effect", effects);
        state.optJSONArray("log").put(entry);
        put(state, "last", aftermath);
        state.remove("timeLeft");
        if (!finalStep) {
            put(state, "step", step() + 1);
            if (ch.optJSONObject("links").has(String.valueOf(step()))) put(state, "page", "link");
        } else if (currentAct < 5) {
            put(state, "branch", state.optInt("branch") * 2 + index);
            put(state, "act", currentAct + 1);
            put(state, "step", 0);
            put(state, "page", "intro");
            checkpoint();
        } else {
            String ending = ch.optJSONArray("ends").optString(index);
            String route = ch.optString("key");
            if (index == 0 && "change".equals(route) && has("poweredRadio")) {
                ending = "together";
                aftermath = "민서는 무전으로 새 갈림길을 듣고 달려왔다. 둘은 같은 문을 건넜다.";
                put(entry, "after", aftermath);
            } else if (index == 0 && "streetProof".equals(route)
                    && (!has("proofPlace") || !has("proofTime"))) {
                ending = "receipt";
                aftermath = "두 사람은 지상에 나왔지만, 밖에서 확인할 수 있는 항목이 기록에 빠져 있었다.";
                put(entry, "after", aftermath);
            } else if ("dawn".equals(ending) && !has("poweredRadio") && (!has("radio") || !ownsBattery())) {
                put(flags, "outsideListener", true);
            }
            put(state, "last", aftermath);
            put(state, "ending", ending);
            put(state, "page", "ending");
        }
        return motion;
    }

    JSONObject endingRecord() {
        JSONObject ending = story.endings.get(state.optString("ending"));
        JSONObject record = new JSONObject();
        put(record, "id", ending.optString("id"));
        put(record, "title", ending.optString("title"));
        put(record, "route", chapter().optString("name"));
        put(record, "type", ending.optString("type"));
        put(record, "art", ending.optInt("art"));
        put(record, "setting", ending.optInt("setting"));
        String primary = ending.optString("body");
        if ("dawn".equals(ending.optString("id")) && has("outsideListener")) {
            primary = "도윤은 지상 정류장에서 약속한 시각을 기다렸다. 역 입구가 있던 벽 쪽에서 작은 잡음이 흘러나왔다.";
        }
        String outcome=ending.optString("id"), route=chapter().optString("key");
        boolean three="three".equals(outcome) || ("broadcast".equals(outcome) && "retrieve".equals(route));
        boolean withMinseo=three || "together".equals(outcome) || "broadcast".equals(outcome)
                || "safe".equals(outcome) || "train".equals(outcome) && has("aboardTogether")
                || "receipt".equals(outcome) && "streetProof".equals(route) && has("finalLeft");
        put(record,"withMinseo",withMinseo);
        put(record,"withJian",three);
        if (three && "broadcast".equals(outcome))
            primary="다시 00:11. 민서는 수신기를 지켰고 지안은 방송실 문을 열고 승강장을 살폈다. 새 열차가 다가오자 도윤이 마이크를 켰다.";
        if (withMinseo && "receipt".equals(outcome))
            primary="민서는 도윤 옆에 앉아 정류장 이름을 불러 줬다. 두 사람이 확인한 내용만 적어 보내고 한참을 기다린 뒤, 도윤이 휴대폰을 열었다.";
        JSONArray variants = ending.optJSONArray("alt");
        String epilogue = "";
        String variant = "default";
        for (int i = 0; i < variants.length(); i++) {
            JSONArray option = variants.optJSONArray(i);
            if (has(option.optString(0)) || "default".equals(option.optString(0))) {
                variant = option.optString(0);
                epilogue = option.optString(1);
                break;
            }
        }
        put(record, "variant", variant);
        put(record, "body", last() + "\n\n" + primary + "\n\n" + epilogue);
        JSONArray log = state.optJSONArray("log");
        JSONArray influences = new JSONArray();
        JSONObject lastChoice = log.optJSONObject(log.length() - 1);
        JSONObject earlier = null;
        for (int i = log.length() - 2; i >= 0; i--) {
            JSONObject entry = log.optJSONObject(i);
            if (earlier == null && entry.optBoolean("fork")) {
                earlier = entry;
            }
        }
        if (earlier != null) influences.put(earlier);
        String fact = "default".equals(variant) ? "power" : variant;
        JSONObject contributingChoice = null;
        for (int i = log.length() - 2; i >= 0; i--) {
            JSONObject entry = log.optJSONObject(i);
            if (entry == earlier || entry.optBoolean("fork")) continue;
            if (entry.optString("effect").contains(fact)) {
                contributingChoice = entry;
                break;
            }
            if (contributingChoice == null && entry.optBoolean("important")) contributingChoice = entry;
        }
        if (contributingChoice != null) influences.put(contributingChoice);
        if (lastChoice != null) influences.put(lastChoice);
        put(record, "influences", influences);
        return record;
    }
}
