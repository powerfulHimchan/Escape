package com.powerfulhimchan.escape;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class Story {
    static final String[] ACT_TITLES = {
            "노선도에 없는 역", "다시 00:11", "따라오는 사람", "약속 장소", "문이 열리는 몇 초"
    };
    static final int[] LENGTHS = {8, 10, 11, 12, 9};

    final JSONObject[][] chapters = new JSONObject[5][];
    final Map<String, JSONObject> endings = new LinkedHashMap<>();

    Story(Context context) throws IOException, JSONException {
        final String source;
        try (InputStream input = context.getAssets().open("story.json");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, read);
            }
            source = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        JSONObject graph = new JSONObject(source);
        for (int act = 0; act < chapters.length; act++) {
            chapters[act] = new JSONObject[1 << act];
        }
        JSONArray list = graph.getJSONArray("chapters");
        for (int i = 0; i < list.length(); i++) {
            JSONObject chapter = list.getJSONObject(i);
            chapters[chapter.getInt("act") - 1][chapter.getInt("index")] = chapter;
        }
        JSONArray outcomes = graph.getJSONArray("endings");
        for (int i = 0; i < outcomes.length(); i++) {
            JSONObject ending = outcomes.getJSONObject(i);
            endings.put(ending.getString("id"), ending);
        }
    }

    JSONObject chapter(int act, int branch) {
        return chapters[act - 1][branch];
    }

    static JSONObject beat(JSONObject chapter, int step) {
        return chapter.optJSONArray("beats").optJSONObject(step);
    }

    static JSONObject option(JSONObject beat, int index) {
        return beat.optJSONArray("choices").optJSONObject(index);
    }
}
