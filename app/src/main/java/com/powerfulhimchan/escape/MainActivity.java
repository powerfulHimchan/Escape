package com.powerfulhimchan.escape;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Insets;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.widget.ImageView;
import android.widget.GridLayout;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int BACKGROUND = 0xFFE5CFAE;
    private static final int PAPER = 0xFFF6EAD5;
    private static final int INK = 0xFF402E25;
    private static final int NOTE = 0xFF963F2D;
    private static final int BORDER = 0xFFA47D64;
    private static final String SAVE = "story.save";
    private static final String ARCHIVE = "story.archive";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Story story;
    private Game game;
    private SharedPreferences preferences;
    private JSONObject archive = new JSONObject();
    private boolean home = true;
    private boolean moving;
    private boolean resumed;
    private boolean hasSave;
    private long clockEnd = -1;
    private long clockDuration;
    private int lastTickSecond = -1;
    private AlertDialog dialog;

    private Typeface regularFont;
    private Typeface boldFont;
    private TextView brand;
    private View seam;
    private Button inventory;
    private Button collection;
    private StationArt art;
    private ScrollView scroll;
    private TextView chapter;
    private TextView title;
    private TextView body;
    private TextView note;
    private TextView timerText;
    private ProgressBar timerBar;
    private Button left;
    private Button right;
    private final Runnable tick = this::tickClock;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        regularFont = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_regular.ttf");
        boldFont = Typeface.createFromAsset(getAssets(), "fonts/noto_sans_kr_bold.ttf");
        preferences = getSharedPreferences("escape", MODE_PRIVATE);
        try {
            story = new Story(this);
            archive = readArchive(preferences.getString(ARCHIVE, "{}"));
        } catch (Exception error) {
            TextView failure = label("이야기 파일을 열지 못했습니다.", 18, true);
            failure.setPadding(dp(24), dp(36), dp(24), dp(24));
            setContentView(failure);
            return;
        }
        hasSave = preferences.contains(SAVE);
        game = new Game(story, preferences.getString(SAVE, ""));
        home = savedInstanceState == null || savedInstanceState.getBoolean("home", true);
        buildLayout();
        render();
    }

    private static JSONObject readArchive(String json) {
        try {
            return new JSONObject(json);
        } catch (Exception invalidArchive) {
            return new JSONObject();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("home", home);
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        if (game != null) startClock();
    }

    @Override protected void onPause() {
        resumed = false;
        if (game != null) pauseClock();
        super.onPause();
    }

    @Override protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
        }
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (game == null || home) {
            super.onBackPressed();
        } else {
            goHome();
        }
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private LinearLayout column() {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        return list;
    }

    private TextView label(String value, float size, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(INK);
        text.setTypeface(bold ? boldFont : regularFont);
        text.setIncludeFontPadding(false);
        return text;
    }

    private GradientDrawable paper(int color, int stroke, int radius) {
        GradientDrawable box = new GradientDrawable();
        box.setColor(color);
        box.setCornerRadius(dp(radius));
        if (stroke != 0) box.setStroke(dp(1), stroke);
        return box;
    }

    private RippleDrawable paperButton(int fill, boolean stripe) {
        GradientDrawable surface = paper(fill, BORDER, 3);
        if (!stripe) {
            return new RippleDrawable(ColorStateList.valueOf(0x25963F2D), surface, null);
        }
        GradientDrawable edge = new GradientDrawable();
        float radius = dp(3);
        edge.setCornerRadii(new float[]{radius, radius, 0, 0, 0, 0, radius, radius});
        edge.setColor(NOTE);
        LayerDrawable layers = new LayerDrawable(new Drawable[]{surface, edge});
        layers.setLayerWidth(1, dp(5));
        layers.setLayerGravity(1, Gravity.START | Gravity.FILL_VERTICAL);
        return new RippleDrawable(ColorStateList.valueOf(0x25963F2D), layers, null);
    }

    private Button button(float size, boolean compact) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setTextSize(size);
        button.setTypeface(boldFont);
        button.setIncludeFontPadding(false);
        button.setTextColor(INK);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(compact ? 48 : 54));
        button.setMinimumHeight(dp(compact ? 48 : 54));
        button.setPadding(dp(compact ? 10 : 16), dp(8), dp(compact ? 10 : 16), dp(8));
        button.setGravity(compact ? Gravity.CENTER : Gravity.CENTER_VERTICAL | Gravity.START);
        button.setBackground(paperButton(compact ? 0xFFEEDBC0 : 0xFFF9ECD7, !compact));
        return button;
    }

    private void add(LinearLayout parent, View child, int gap) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(gap);
        parent.addView(child, params);
    }

    @SuppressWarnings("deprecation")
    private void buildLayout() {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(BACKGROUND);
        // Install the decor view before calling Android's window inset APIs.
        setContentView(outer);

        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            outer.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets safe = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
                WindowInsetsController controller = view.getWindowInsetsController();
                if (controller != null) {
                    int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                    controller.setSystemBarsAppearance(mask, mask);
                }
                return insets;
            });
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        outer.requestApplyInsets();

        LinearLayout layout = column();
        layout.setPadding(dp(16), 0, dp(16), 0);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels, dp(600));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, -1, Gravity.CENTER_HORIZONTAL);
        outer.addView(layout, layoutParams);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        brand = label("막차가 떠난 뒤", 25, true);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        brand.setMinHeight(dp(48));
        brand.setContentDescription("막차가 떠난 뒤, 제목 화면");
        brand.setOnClickListener(v -> goHome());
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));
        collection = button(14, true);
        collection.setText("엔딩");
        collection.setOnClickListener(v -> showArchive());
        header.addView(collection, new LinearLayout.LayoutParams(-2, -2));
        inventory = button(14, true);
        inventory.setText("소지품");
        inventory.setOnClickListener(v -> showInventory());
        LinearLayout.LayoutParams inventoryParams = new LinearLayout.LayoutParams(-2, -2);
        inventoryParams.setMarginStart(dp(6));
        header.addView(inventory, inventoryParams);
        add(layout, header, 10);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        layout.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout contents = column();
        contents.setPadding(0, 0, 0, dp(12));
        scroll.addView(contents, new ScrollView.LayoutParams(-1, -2));

        FrameLayout picture = new FrameLayout(this);
        picture.setBackground(paper(PAPER, BORDER, 3));
        picture.setPadding(dp(5), dp(5), dp(5), dp(5));
        picture.setElevation(dp(2));
        picture.setRotation(-1.2f);
        art = new StationArt(this);
        int pictureHeight = (width - dp(48)) / 2;
        picture.addView(art, new FrameLayout.LayoutParams(-1, pictureHeight));
        LinearLayout.LayoutParams photo = new LinearLayout.LayoutParams(-1, -2);
        photo.setMargins(dp(3), dp(14), dp(3), 0);
        contents.addView(picture, photo);

        LinearLayout card = column();
        card.setPadding(dp(22), dp(12), dp(14), dp(14));
        chapter = label("", 13, true);
        chapter.setTextColor(NOTE);
        add(card, chapter, 0);
        title = label("", 24, true);
        if (Build.VERSION.SDK_INT >= 28) title.setAccessibilityHeading(true);
        add(card, title, 4);
        body = label("", 18, false);
        body.setLineSpacing(dp(6), 1f);
        add(card, body, 10);
        note = label("", 14, false);
        note.setTextColor(NOTE);
        note.setLineSpacing(dp(4), 1f);
        add(card, note, 12);
        FrameLayout page = new FrameLayout(this);
        page.setBackground(paper(PAPER, BORDER, 3));
        page.setElevation(dp(2));
        page.addView(card, new FrameLayout.LayoutParams(-1, -2));
        seam = new View(this);
        GradientDrawable stitching = new GradientDrawable();
        stitching.setStroke(dp(1), BORDER, dp(2), dp(2));
        seam.setBackground(stitching);
        FrameLayout.LayoutParams seamPosition = new FrameLayout.LayoutParams(dp(1), -1, Gravity.START);
        seamPosition.setMargins(dp(10), dp(12), 0, dp(12));
        page.addView(seam, seamPosition);
        ImageView bookmark = new ImageView(this);
        bookmark.setImageResource(R.drawable.journal_bookmark);
        bookmark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams tab = new FrameLayout.LayoutParams(dp(14), dp(23), Gravity.TOP | Gravity.END);
        tab.setMarginEnd(dp(17));
        page.addView(bookmark, tab);
        add(contents, page, 14);

        LinearLayout footer = column();
        footer.setPadding(0, dp(4), 0, dp(10));
        timerText = label("", 13, true);
        timerText.setTextColor(NOTE);
        timerText.setGravity(Gravity.END);
        add(footer, timerText, 0);
        timerBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        timerBar.setMax(1000);
        timerBar.setProgressTintList(ColorStateList.valueOf(NOTE));
        timerBar.setProgressBackgroundTintList(ColorStateList.valueOf(BORDER));
        timerBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        footer.addView(timerBar, new LinearLayout.LayoutParams(-1, dp(6)));
        left = button(16, false);
        right = button(16, false);
        add(footer, left, 0);
        add(footer, right, 8);
        add(layout, footer, 0);
    }

    private void bind(Button target, String text, Runnable action) {
        boolean visible = action != null;
        target.setVisibility(visible ? View.VISIBLE : View.GONE);
        target.setEnabled(visible);
        if (visible) {
            target.setText(text);
            target.setOnClickListener(v -> action.run());
        }
    }

    private void resetVisualClock() {
        timerText.setVisibility(View.GONE);
        timerBar.setVisibility(View.GONE);
        art.countdown(-1, 1, false);
    }

    private void showText(String phase, String heading, String text, String details) {
        brand.setVisibility(home ? View.INVISIBLE : View.VISIBLE);
        seam.setVisibility(home ? View.GONE : View.VISIBLE);
        title.setTextSize(home ? 30 : 24);
        title.setGravity(home ? Gravity.CENTER : Gravity.START);
        body.setGravity(home ? Gravity.CENTER : Gravity.START);
        chapter.setVisibility(home ? View.GONE : View.VISIBLE);
        chapter.setText(phase);
        title.setText(heading);
        body.setText(text);
        note.setText(details);
        note.setVisibility(details.isEmpty() ? View.GONE : View.VISIBLE);
        scroll.scrollTo(0, 0);
        scroll.post(() -> scroll.scrollTo(0, 0));
    }

    private void render() {
        resetVisualClock();
        collection.setContentDescription("엔딩 모아보기");
        inventory.setVisibility(home ? View.GONE : View.VISIBLE);
        if (home) {
            art.configure(0, false, false, false, false, false, true);
            showText("선택형 탈출 이야기", getString(R.string.app_name),
                    "00:11. 막차가 노선도에 없는 역에 멈췄다.\n\n"
                            + "셔터가 내려오기 시작했다. 어떤 걸 챙기고, 어느 쪽으로 달릴까?", "");
            if (hasSave) {
                bind(left, "이어하기", () -> {
                    home = false;
                    render();
                });
                bind(right, "새 이야기", this::confirmNewStory);
            } else {
                bind(left, "이야기 시작", this::newStory);
                bind(right, "", null);
            }
            return;
        }
        JSONObject current = game.chapter();
        int currentAct = game.act();
        String phase = currentAct + "막 · " + Story.ACT_TITLES[currentAct - 1];
        String route = current.optString("key");
        boolean intro = "intro".equals(game.page());
        int scene = intro ? current.optInt("setting") : game.beat().optInt("setting");
        boolean closeUp = "link".equals(game.page()) || scene == 1 || route.equals("hall") && game.step() == 0;
        boolean nearby = currentAct >= 3 && !route.equals("trainSolo") && !route.equals("change")
                && !route.equals("retrieve") && !route.equals("partial") && !route.equals("climb")
                && !route.equals("correct") && !(route.equals("oldstairs") && game.step() > 0)
                && !(route.equals("voice") && game.step() == 0) && !(route.equals("records") && intro);
        boolean rider = route.equals("gather") || route.equals("retrieve")
                || route.equals("rider") && game.step() > 0;
        boolean start = route.equals("start");
        boolean choice = game.page().equals("choice");
        int step = game.step();
        boolean timedShutter = start && choice && step == 1;
        boolean timed = choice && game.beat().optInt("seconds") > 0;
        if (timed) closeUp = true;
        int panel = -1;
        if (start && !intro) {
            if (choice && step == 0) panel = 0;
            else if (!choice && step == 1) panel = 1;
            else if (choice && step == 4) panel = 2;
            else if (step >= 5) panel = 3;
        }
        art.configure(scene, closeUp, nearby, rider, timedShutter && !game.has("radio"),
                game.has("poweredLamp"), true);
        art.stage(start && intro, route.equals("train") && intro,
                currentAct < 4 && scene == 0, timed, timedShutter, panel == 3 || scene == 2, panel);
        switch (game.page()) {
            case "intro":
                String opening = current.optString("opening");
                if (route.equals("hall") && game.has("handprint"))
                    opening += "\n\n기둥 옆면에 아까 본 손바닥 모양 얼룩이 있었다. 같은 자리였다.";
                if (route.equals("records") && game.has("zeroCount"))
                    opening += "\n\n일지에도 하차 인원은 0명. 개찰구 전광판에서 본 숫자와 같았다.";
                showText(phase, current.optString("name"), opening, "");
                bind(left, "계속", this::continueScene);
                bind(right, "", null);
                break;
            case "link":
                showText(phase, current.optString("name"), withLast(current.optJSONObject("links").optString(String.valueOf(game.step()))), "");
                bind(left, "계속", this::continueScene);
                bind(right, "", null);
                break;
            case "ending":
                showEnding(game.endingRecord());
                break;
            default:
                showChoice(phase);
                break;
        }
    }

    private String withLast(String text) {
        if (game.last().isEmpty() || game.step() == 0 || game.chapter().optJSONObject("links").has(String.valueOf(game.step())) && !"link".equals(game.page())) {
            return text;
        }
        return game.last() + "\n\n" + text;
    }

    private void showChoice(String phase) {
        JSONObject beat = game.beat();
        int seconds = beat.optInt("seconds");
        String details = game.timerClue();
        String text = beat.optString("body");
        if ("power".equals(beat.optString("mode"))) {
            if (game.has("poweredRadio") || game.has("poweredLamp")) {
                text = "건전지를 넣은 장비의 신호를 확인할 수 있다. 다른 장비 대신 통로에 남은 표시를 살펴볼 수도 있다.";
            } else if (!game.has("battery")) {
                text = "남은 건전지가 없다. 고정 호출기의 위치를 확인하거나 비상등이 비추는 방향을 기억할 수 있다.";
            }
        }
        if ("power".equals(beat.optString("mode"))) {
            text += " 호출기와 비상등은 갈라지는 통로 양쪽에 있다. 문이 열리는 동안 한쪽으로만 건널 수 있다.";
        }
        showText(phase, beat.optString("title"), withLast(text), details);
        bind(left, game.choiceLabel(0), () -> select(0));
        bind(right, game.choiceLabel(1), () -> select(1));
        if (seconds > 0) {
            if (!game.state.has("timeLeft")) {
                Game.put(game.state, "timeLeft", seconds * 1000L);
                save();
            }
            timerText.setVisibility(View.VISIBLE);
            timerBar.setVisibility(View.VISIBLE);
            displayClock(game.state.optLong("timeLeft"), seconds * 1000L);
            startClock();
        }
    }

    private void continueScene() {
        game.continueScene();
        save();
        render();
    }

    private void select(int index) {
        if (moving || home || !"choice".equals(game.page())) return;
        if (clockEnd >= 0 && SystemClock.elapsedRealtime() >= clockEnd) index = -1;
        handler.removeCallbacks(tick);
        clockEnd = -1;
        left.setEnabled(false);
        right.setEnabled(false);
        moving = true;
        JSONObject option = index < 0 ? null : Story.option(game.beat(), index);
        String motion = game.select(index);
        art.play(motion, option == null ? "hold" : option.optString("minseoMotion", "hold"),
                option == null ? "hold" : option.optString("jianMotion", "hold"));
        if (index < 0) art.countdown(0, Math.max(1, clockDuration), false);
        if ("ending".equals(game.page())) archiveEnding(game.endingRecord());
        save();
        handler.postDelayed(() -> {
            moving = false;
            render();
        }, StationArt.ACTION_MS + 30);
    }

    private void displayClock(long remaining, long total) {
        long leftMillis = Math.max(0, remaining);
        int seconds = (int) ((leftMillis + 999) / 1000);
        if (lastTickSecond != seconds || timerText.length() == 0) {
            timerText.setText("남은 시간  " + seconds + "초");
            lastTickSecond = seconds;
        }
        timerBar.setProgress((int) (1000 * leftMillis / Math.max(1, total)));
        art.countdown(leftMillis, total, clockEnd >= 0);
    }

    private void startClock() {
        if (!resumed || moving || home || dialog != null || clockEnd >= 0
                || !"choice".equals(game.page()) || !game.state.has("timeLeft")) return;
        clockDuration = game.beat().optInt("seconds") * 1000L;
        if (clockDuration <= 0) return;
        clockEnd = SystemClock.elapsedRealtime() + game.state.optLong("timeLeft");
        tickClock();
    }

    private void tickClock() {
        if (clockEnd < 0) return;
        long remaining = clockEnd - SystemClock.elapsedRealtime();
        displayClock(remaining, clockDuration);
        if (remaining <= 0) {
            select(-1);
        } else {
            handler.postDelayed(tick, Math.min(80, remaining));
        }
    }

    private void pauseClock() {
        if (clockEnd < 0) return;
        long remaining = Math.max(0, clockEnd - SystemClock.elapsedRealtime());
        Game.put(game.state, "timeLeft", remaining);
        clockEnd = -1;
        handler.removeCallbacks(tick);
        displayClock(remaining, clockDuration);
        save();
    }

    private void save() {
        hasSave = true;
        preferences.edit().putString(SAVE, game.serialize()).apply();
    }

    private void goHome() {
        pauseClock();
        home = true;
        render();
    }

    private void newStory() {
        moving = false;
        game.restart();
        home = false;
        save();
        render();
    }

    private void confirmNewStory() {
        present(new AlertDialog.Builder(this)
                .setTitle("새 이야기")
                .setMessage("현재 진행 기록을 지우고 처음부터 시작합니다. 모은 엔딩은 남습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("처음부터", (whichDialog, which) -> newStory()));
    }

    private void present(AlertDialog.Builder builder) {
        pauseClock();
        dialog = builder.create();
        dialog.setOnDismissListener(dismissed -> {
            if (dialog == dismissed) dialog = null;
            startClock();
        });
        dialog.show();
        styleDialog(dialog.getWindow().getDecorView());
    }

    private void styleDialog(View view) {
        if (view instanceof TextView) {
            TextView text = (TextView) view;
            boolean bold = view instanceof Button || text.getTypeface() != null && text.getTypeface().isBold();
            text.setTypeface(bold ? boldFont : regularFont);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i=0; i<group.getChildCount(); i++) styleDialog(group.getChildAt(i));
        }
    }

    private void showInventory() {
        LinearLayout list = column();
        list.setPadding(dp(22), dp(4), dp(22), dp(15));
        String[] keys = {"radio","battery","lamp","map"};
        String[] names = {"무전기","건전지","손전등","역 안내도"};
        String[] details = {
                game.has("poweredRadio") ? "전원이 들어와 있다. 떨어진 곳에 신호를 보낼 수 있다." : "사용하려면 건전지가 필요하다.",
                game.has("poweredLamp") ? "손전등에 장착했다." : game.has("poweredRadio") ? "무전기에 장착했다." : "장비 하나에 넣을 수 있다.",
                game.has("poweredLamp") ? "정전 통로에서 불빛으로 신호를 보낼 수 있다." : "건전지를 넣으면 어두운 통로를 비춘다.",
                game.has("mark") ? "갈림길에 직접 남긴 표시가 있다." : "옛 출구와 통로를 확인할 수 있다."
        };
        int[] icons = {R.drawable.item_radio,R.drawable.item_battery,R.drawable.item_lamp,R.drawable.item_map};
        GridLayout slots = new GridLayout(this);
        slots.setColumnCount(8);
        slots.setRowCount(8);
        add(list, slots, 0);
        TextView selected = label("",17,true);
        TextView description = label("아직 소지품이 없다.",15,false);
        add(list, selected, 10);
        add(list, description, 6);
        int nextItem = 0;
        for (int cell=0; cell<64; cell++) {
            while (nextItem<keys.length && !game.has(keys[nextItem])) nextItem++;
            View slot;
            boolean filled = nextItem<keys.length;
            if (filled) {
                int index = nextItem++;
                ImageView icon = new ImageView(this);
                icon.setImageResource(icons[index]);
                icon.setPadding(dp(4),dp(4),dp(4),dp(4));
                icon.setContentDescription(names[index]);
                icon.setOnClickListener(v -> {
                    selected.setText(names[index]);
                    description.setText(details[index]);
                });
                description.setText("아이콘을 누르면 설명을 볼 수 있어.");
                slot = icon;
            } else {
                slot = new View(this);
                slot.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            }
            slot.setBackground(paper(filled ? 0xFFF9ECD7 : 0xFFF0DFC3,filled ? BORDER : 0x55A47D64,3));
            GridLayout.LayoutParams pos = new GridLayout.LayoutParams(
                    GridLayout.spec(cell/8), GridLayout.spec(cell%8,1,1f));
            pos.width = 0;
            pos.height = dp(28);
            pos.setMargins(dp(2),dp(2),dp(2),dp(2));
            slots.addView(slot,pos);
        }
        ScrollView listScroll = new ScrollView(this);
        listScroll.addView(list);
        present(new AlertDialog.Builder(this).setTitle("소지품").setView(listScroll)
                .setPositiveButton("닫기", null));
    }


    private void archiveEnding(JSONObject record) {
        String id = record.optString("id");
        JSONArray results = archive.optJSONArray(id);
        if (results == null) {
            results = new JSONArray();
            Game.put(archive, id, results);
        }
        for (int i = 0; i < results.length(); i++) {
            JSONObject existing = results.optJSONObject(i);
            if (existing.optString("body").equals(record.optString("body"))) return;
        }
        results.put(record);
        preferences.edit().putString(ARCHIVE, archive.toString()).apply();
    }

    private String endingBody(JSONObject record) {
        JSONArray influence = record.optJSONArray("influences");
        StringBuilder text = new StringBuilder(record.optString("body"));
        if (influence != null && influence.length() > 0) {
            text.append("\n\n결말에 영향을 준 선택");
            for (int i = 0; i < influence.length(); i++) {
                JSONObject entry = influence.optJSONObject(i);
                text.append("\n• ").append(entry.optInt("act")).append("막 · ")
                        .append(entry.optString("label")).append("\n  ").append(entry.optString("after"));
            }
        }
        return text.toString();
    }

    private void showEnding(JSONObject ending) {
        String category = ending.optString("type");
        boolean three = "three".equals(ending.optString("id"));
        String outcome = ending.optString("id");
        boolean together = three || outcome.equals("together") || outcome.equals("broadcast")
                || outcome.equals("safe") || outcome.equals("train") && game.has("aboardTogether");
        art.configure(ending.optInt("setting"), false, together, three, false, game.has("poweredLamp"), true);
        showText("엔딩" + ("기존".equals(category) ? "" : " · " + category),
                ending.optString("title"), endingBody(ending), "");
        bind(left, "원하는 막부터 다시 보기", this::chooseAct);
        bind(right, "제목으로", this::goHome);
    }

    private void showArchive() {
        List<String> ids = new ArrayList<>(story.endings.keySet());
        CharSequence[] labels = new CharSequence[ids.size()];
        int count = 0;
        for (int i = 0; i < ids.size(); i++) {
            JSONObject base = story.endings.get(ids.get(i));
            JSONArray seen = archive.optJSONArray(ids.get(i));
            String category = base.optString("type");
            String prefix = "기존".equals(category) ? "" : category + " · ";
            if (seen != null && seen.length() > 0) {
                count++;
                labels[i] = prefix + base.optString("title");
            } else {
                labels[i] = prefix + "미발견 엔딩";
            }
        }
        present(new AlertDialog.Builder(this).setTitle("엔딩 모아보기  " + count + "/" + ids.size())
                .setItems(labels, (menu, index) -> {
                    JSONArray records = archive.optJSONArray(ids.get(index));
                    if (records != null && records.length() > 0) handler.post(() -> showRecords(records));
                }).setPositiveButton("닫기", null));
    }

    private void showRecords(JSONArray records) {
        JSONObject first = records.optJSONObject(0);
        if (records.length() == 1) {
            showArchivedEnding(first);
            return;
        }
        CharSequence[] variants = new CharSequence[records.length()];
        for (int i = 0; i < variants.length; i++) {
            variants[i] = (i + 1) + ". " + records.optJSONObject(i).optString("route");
        }
        present(new AlertDialog.Builder(this).setTitle(first.optString("title"))
                .setItems(variants, (menu, index) -> handler.post(() -> showArchivedEnding(records.optJSONObject(index))))
                .setPositiveButton("닫기", null));
    }

    private void showArchivedEnding(JSONObject record) {
        present(new AlertDialog.Builder(this)
                .setTitle(record.optString("title"))
                .setMessage(endingBody(record))
                .setPositiveButton("닫기", null));
    }

    private void chooseAct() {
        CharSequence[] names = new CharSequence[game.checkpoints.length()];
        for (int i = 0; i < names.length; i++) {
            JSONObject snapshot = game.checkpoints.optJSONObject(i);
            JSONObject route = story.chapter(i + 1, snapshot.optInt("branch"));
            names[i] = (i + 1) + "막 · " + route.optString("name");
        }
        present(new AlertDialog.Builder(this).setTitle("다시 볼 막")
                .setItems(names, (menu, index) -> {
                    if (game.replay(index + 1)) {
                        save();
                        render();
                    }
                }).setNegativeButton("닫기", null));
    }
}
