package com.powerfulhimchan.escape;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

final class StationArt extends View {
    private static final int WIDTH = 288;
    private static final int HEIGHT = 170;
    private static final int[][] PALETTES = {
            {0x172237, 0x536984, 0x3B516E, 0x263B56, 0xF5C488},
            {0x232333, 0x5E6879, 0x424D63, 0x30384B, 0xE4A46F},
            {0x152636, 0x567989, 0x3A586D, 0x233C52, 0xE6CB9D},
            {0x172334, 0x526B8F, 0x415674, 0x293A56, 0xF3B06F},
            {0x181E30, 0x333F57, 0x27344A, 0x1A273C, 0xEA8180},
            {0x22202F, 0x5D657A, 0x414C62, 0x2D394D, 0xD99976},
            {0x242735, 0x566B81, 0x40556B, 0x2B3D57, 0xFFE5AB},
            {0x202334, 0x535D77, 0x3B4965, 0x293650, 0xE2A47C},
            {0x252945, 0x667189, 0x475A73, 0x354760, 0xFFD6A3},
            {0x192238, 0x576D8D, 0x405676, 0x293D59, 0xFFBE7A}
    };

    private final Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
    private final Canvas pixels = new Canvas(bitmap);
    private final Paint paint = new Paint(0);
    private final Paint scalePaint = new Paint(0);
    private final RectF bounds = new RectF();
    private final Path path = new Path();

    private int location;
    private int act;
    private boolean third;
    private boolean radioTaken;
    private boolean lamp;
    private boolean clockRunning;
    private long timeLeft = -1;
    private long timeTotal = 1;
    private long moveAt = -1000;
    private String move = "hold";
    private int[] palette = PALETTES[0];

    StationArt(Context context) {
        super(context);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        scalePaint.setFilterBitmap(false);
    }

    void configure(int location, int act, boolean third, boolean radioTaken, boolean lamp) {
        this.location = Math.max(0, Math.min(location, PALETTES.length - 1));
        this.act = act;
        this.third = third;
        this.radioTaken = radioTaken;
        this.lamp = lamp;
        palette = PALETTES[this.location];
        invalidate();
    }

    void countdown(long remaining, long total, boolean running) {
        timeLeft = remaining;
        timeTotal = Math.max(total, 1);
        clockRunning = running;
        invalidate();
    }

    void play(String motion) {
        move = motion;
        moveAt = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long time = SystemClock.uptimeMillis();
        float progress = Math.min(1, (time - moveAt) / 820f);
        pixels.drawColor(color(palette[0]));
        sky(time);
        ground();
        if (location == 8) {
            street(time);
        } else {
            walls(time);
        }

        if (location == 5) drawTrain(time);
        else if (location == 7) drawStairs();
        else if (location == 1 || location == 9 || location == 0) drawOffice();
        if (location == 9 || location == 3 || location == 6) shutter(time);
        if (location == 4) tunnel(time);
        if (location == 8) bench();

        if (act >= 3 && location != 0 && (act < 5 || location != 1 || third)) {
            person(3.8f, 2.5f, 0xAC7773, 0x344058, time, false, false);
        }
        if (third) person(4.7f, 2.8f, 0x9DAAB8, 0x404C64, time + 160, false, false);

        float u = 3.6f;
        float v = 4.7f;
        boolean moving = progress < 1 && !"hold".equals(move);
        boolean run = move.startsWith("run");
        float amount = progress * progress * (3 - 2 * progress);
        if (progress < 1) {
            if ("pick".equals(move)) {
                u += .55f * amount;
                v -= 1.0f * amount;
            } else if ("left".equals(move) || "runLeft".equals(move)) {
                u -= 2.1f * amount;
                v -= .9f * amount;
            } else if ("right".equals(move) || "runRight".equals(move)) {
                u += 1.35f * amount;
                v -= 1.9f * amount;
            }
        }
        if (lamp && location == 4) lampBeam(u, v, time);
        person(u, v, 0x687C57, 0x303746, time, moving, run && location == 9 && progress > .46f);
        if ("pick".equals(move) && progress > .55f && progress < .88f) {
            int x = ix(u, v), y = iy(u, v) - 25;
            rect(x + 2, y, x + 6, y + 5, palette[4]);
            rect(x + 6, y - 1, x + 8, y + 1, shade(palette[4], 18));
            rect(x, y - 3, x + 1, y + 1, palette[4]);
        }

        bounds.set(0, 0, getWidth(), getHeight());
        canvas.drawBitmap(bitmap, null, bounds, scalePaint);
        if (isShown()) postInvalidateDelayed(moving || clockRunning ? 32 : 125);
    }

    private static int color(int rgb) { return 0xFF000000 | rgb; }

    private static int shade(int rgb, int amount) {
        int r = Math.min(255, Math.max(0, ((rgb >> 16) & 255) + amount));
        int g = Math.min(255, Math.max(0, ((rgb >> 8) & 255) + amount));
        int b = Math.min(255, Math.max(0, (rgb & 255) + amount));
        return (r << 16) | (g << 8) | b;
    }

    private static int ix(float u, float v) { return Math.round(141 + (u - v) * 14); }
    private static int iy(float u, float v) { return Math.round(56 + (u + v) * 6.5f); }

    private void rect(int l, int t, int r, int b, int rgb) {
        paint.setColor(color(rgb));
        pixels.drawRect(l, t, r, b, paint);
    }

    private void poly(int rgb, int... points) {
        if (points.length < 6) return;
        path.rewind();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.close();
        paint.setColor(color(rgb));
        pixels.drawPath(path, paint);
    }

    private void plane(float u, float v, float a, float b, int lift, int rgb) {
        poly(rgb, ix(u,v), iy(u,v)-lift, ix(u+a,v), iy(u+a,v)-lift,
                ix(u+a,v+b), iy(u+a,v+b)-lift, ix(u,v+b), iy(u,v+b)-lift);
    }

    private void sideU(float u, float v, float a, int low, int high, int rgb) {
        poly(rgb, ix(u,v), iy(u,v)-low, ix(u+a,v), iy(u+a,v)-low,
                ix(u+a,v), iy(u+a,v)-high, ix(u,v), iy(u,v)-high);
    }

    private void sideV(float u, float v, float a, int low, int high, int rgb) {
        poly(rgb, ix(u,v), iy(u,v)-low, ix(u,v+a), iy(u,v+a)-low,
                ix(u,v+a), iy(u,v+a)-high, ix(u,v), iy(u,v)-high);
    }

    private void block(float u, float v, float a, float b, int low, int high, int rgb) {
        sideU(u,v+b,a,low,high,shade(rgb,-14));
        sideV(u+a,v,b,low,high,shade(rgb,-30));
        plane(u,v,a,b,high,shade(rgb,13));
    }

    private void sky(long time) {
        for (int y = 0; y < HEIGHT; y += 4) {
            rect(0,y,WIDTH,y+4,shade(palette[0], Math.max(0, 17 - y / 6)));
        }
        if (location != 8) return;
        for (int i = 0; i < 25; i++) {
            int x = (i * 37 + 9) % 288;
            int y = (i * 17 + 4) % 49;
            if (i % 4 != 0 || (time / 900) % 2 == 0) rect(x,y,x+1,y+1,0x979AB0);
        }
    }

    private void ground() {
        sideU(-.1f, 7.1f, 7.2f, -7, 0, shade(palette[2], -26));
        sideV(7.1f, -.1f, 7.2f, -7, 0, shade(palette[3], -20));
        plane(-.1f,-.1f,7.2f,7.2f,0,shade(palette[3],-18));
        for (int v = 0; v < 7; v++) {
            for (int u = 0; u < 7; u++) {
                int variance = (u * 13 + v * 19 + location * 7) % 16 - 7;
                int tile = shade(palette[2], variance);
                boolean aisle = location != 1 && (u == 4 || v == 5 && location != 4);
                if (aisle) tile = shade(palette[4], -56 + variance);
                plane(u + .03f, v + .04f, .94f, .92f, 0, tile);
                if (aisle) {
                    plane(u+.27f,v+.23f,.13f,.12f,1,shade(tile,30));
                    plane(u+.61f,v+.53f,.13f,.12f,1,shade(tile,33));
                } else if ((u + v * 2) % 4 == 0) {
                    plane(u+.12f,v+.15f,.43f,.22f,1,shade(tile,10));
                }
            }
        }
    }

    private void walls(long time) {
        int left = palette[2], right = palette[1];
        for (int i = 0; i < 7; i++) {
            for (int z = 0; z < 48; z += 7) {
                int a = z / 7 == 3 ? shade(0xA06B59, -22) : shade(left, (i * 7 + z) % 13 - 7);
                int b = z / 7 == 3 ? shade(0xB37A65, -16) : shade(right, (i * 13 + z) % 15 - 6);
                sideV(0, i+.04f, .93f, z+1, z+7, a);
                if (location != 9 && location != 6 && location != 3 || i < 2 || i > 4) {
                    sideU(i+.04f, 0, .93f, z+1, z+7, b);
                }
            }
        }
        block(-.08f, -.08f, .28f, 7.17f, 48, 51, palette[2]);
        if (location != 9 && location != 6 && location != 3) {
            block(-.08f, -.08f, 7.18f, .26f, 48, 51, palette[1]);
        }
        sideV(.04f,.5f,2.7f,35,40,palette[4]);
        sideV(.06f,.55f,2.58f,36,38,shade(palette[4],23));
        sideU(5.4f,.08f,1.08f,40,43,shade(palette[4],15));
        if (location == 8 || location == 4) return;
        block(.18f,4.85f,.4f,.32f,25,32,0x50384A);
        int lit = (time / 390) % 4 == 0 ? 0xAA534E : 0xF28063;
        sideV(.6f,4.91f,.2f,25,30,lit);
    }

    private void drawOffice() {
        block(4.7f, 1.35f, 1.8f, 1.03f, 0, 12, palette[2]);
        sideU(4.78f,2.4f,1.63f,2,6,shade(palette[1],3));
        block(4.62f,1.26f,1.96f,1.19f,12,15,shade(palette[1],15));
        int x = ix(5.75f, 1.8f), y = iy(5.75f, 1.8f) - 14;
        if (!radioTaken || location == 1) {
            rect(x-6,y-5,x+6,y+2,0x171A2B);
            rect(x-5,y-9,x+4,y-4,0x292E3F);
            rect(x+2,y-13,x+4,y-8,0x626C75);
            rect(x-4,y-7,x+1,y-5,0xEB8563);
            rect(x-4,y-2,x+1,y-1,0x5E687C);
        }
        if (location == 1) {
            block(1,1.15f,1.6f,1f,0,11,0x66575B);
            plane(1.28f,1.45f,.55f,.43f,12,0xF4D5AB);
            plane(1.76f,1.6f,.3f,.22f,12,0x9A7160);
            block(1.13f,2.65f,.63f,.58f,0,7,0x434B60);
        }
    }

    private void shutter(long time) {
        plane(2.05f,.18f,2.96f,2.14f,1,shade(palette[4],-32));
        for (int i = 0; i < 4; i++) {
            plane(2.25f+.69f*i,.26f,.49f,1.4f-i%2*.2f,1,shade(palette[4],-10-i*6));
        }
        if (location == 6) {
            sideU(2.1f,0,2.72f,42,48,0x373E53);
            sideU(3.06f,.04f,.62f,43,45,(time/750)%3 != 0 ? 0xA3CC8B : 0x698669);
        }
        block(1.84f,-.06f,.35f,.44f,0,49,palette[1]);
        block(4.92f,-.06f,.33f,.44f,0,49,palette[1]);
        block(1.87f,-.06f,3.39f,.42f,43,49,palette[2]);
        int low = location == 6 ? 31 : location == 3 ? 14 : 24;
        if (location == 9 && timeLeft >= 0) low = 4 + (int)(29 * (float)timeLeft / timeTotal);
        for (int j = low; j < 43; j += 3) {
            sideU(2.1f,.05f,2.83f,j,Math.min(43,j+3),shade(palette[1],j%8-19));
            sideU(2.1f,.07f,2.83f,j,j+1,shade(palette[2],-20));
        }
        if (location == 3) {
            sideU(2.22f,.14f,2.49f,14,30,0x536A80);
            sideU(2.38f,.15f,.12f,15,28,0x8EA6B4);
            sideU(2.63f,.15f,.28f,19,24,0x647D91);
        }
    }

    private void tunnel(long time) {
        sideU(2.2f,.12f,2.9f,0,34,0x151A25);
        block(2.07f,0,.31f,.4f,0,40,0x43506B);
        block(4.84f,0,.3f,.4f,0,40,0x43506B);
        block(2.1f,0,3.04f,.39f,33,39,0x404B60);
        sideU(3.1f,.15f,.47f,28,31,(time/260)%4 == 0 ? 0x6B4151 : 0xD77775);
        for (int i=0;i<8;i++) plane(3.5f,.28f+i*.6f,.2f,.24f,1,i%2==0 ? 0x867767 : 0x5D5660);
    }

    private void drawTrain(long time) {
        block(.65f,.35f,5.9f,1.75f,1,28,0x7C7D91);
        sideU(.88f,2.12f,5.55f,5,10,0xA56162);
        sideU(.85f,2.12f,5.63f,10,12,0xD9A278);
        for (int i=0;i<5;i++) {
            float u = 1.08f+i*1.03f;
            int tint = i==2 && (time/850)%3!=0 ? 0xE1A875 : 0x2F3C53;
            sideU(u,2.15f,.7f,15,23,tint);
            sideU(u+.1f,2.16f,.48f,21,22,shade(tint,16));
        }
        sideU(3.15f,2.17f,.65f,1,23,0x2A354A);
        sideU(3.23f,2.18f,.48f,4,21,0xC18B63);
        sideV(6.58f,.74f,.85f,15,23,0x323D54);
        sideV(6.61f,1.15f,.3f,5,8,0xF2987B);
    }

    private void drawStairs() {
        for (int i=7;i>=0;i--) {
            float v=1.3f+i*.42f;
            int top=37-4*i;
            block(3.0f,v,2.35f,.43f,0,top,shade(palette[1],i%2==0 ? 0 : -7));
            plane(3.03f,v+.34f,2.29f,.07f,top+1,shade(palette[4],-23));
        }
        block(2.88f,1.36f,.15f,3.2f,27,30,0x9A9095);
    }

    private void street(long time) {
        for (int i=0;i<5;i++) {
            float u=.3f+i*1.32f;
            int h=16+(i*11)%25;
            block(u,0,1.13f,.82f,0,h,shade(palette[2],(i%3-1)*10));
            for (int y=6;y<h-2;y+=7) {
                sideU(u+.17f,.85f,.19f,y,y+3,(i+y)%5!=0 ? 0xC59380 : 0x484361);
                sideU(u+.64f,.85f,.22f,y,y+3,0x74779B);
            }
        }
        block(5.9f,2.42f,.13f,.13f,0,34,0x6B737A);
        block(5.8f,2.15f,.75f,.23f,31,35,0xD6ADB0);
        int light = (time / 800) % 5 == 0 ? 0xD9BB99 : 0xF7D39B;
        plane(5.34f,2.5f,1.28f,1.61f,1,shade(light,-47));
    }

    private void bench() {
        block(1.13f,4.54f,.14f,.32f,0,6,0x414758);
        block(2.85f,4.54f,.14f,.32f,0,6,0x414758);
        block(1.06f,4.42f,2.03f,.59f,5,8,0xA27361);
        sideU(1.06f,4.43f,2.03f,10,14,0xA77B6A);
    }

    private void lampBeam(float u, float v, long time) {
        int x=ix(u,v), y=iy(u,v);
        poly((time/230)%3==0 ? 0x59607B : 0x60677D,x+3,y-6,x+19,y-5,x+33,y+2,x+23,y+10);
        poly(0x82746F,x+7,y-4,x+20,y-3,x+28,y+2,x+21,y+7);
    }

    private void person(float u, float v, int jacket, int pants, long time, boolean moving, boolean duck) {
        int x = ix(u,v), y = iy(u,v);
        int a = moving ? ((time/85)%2 == 0 ? 3 : -3) : 0;
        int dy = duck ? 4 : moving && a > 0 ? 1 : 0;
        poly(shade(palette[3],-13),x-11,y,x+1,y-4,x+12,y,x+3,y+5,x-5,y+4);
        int middle = y - 14 + dy;
        rect(x-5,y-10,x-1,y+a,pants);
        rect(x+1,y-10,x+5,y-a,shade(pants,10));
        rect(x-6,y+a-1,x-1,y+a+2,0x242439);
        rect(x+1,y-a-1,x+7,y-a+2,0x26263A);
        rect(x-5,middle-6,x+5,y-8,jacket);
        rect(x-3,middle-7,x+4,middle-5,shade(jacket,21));
        rect(x-7,middle-4,x-4,middle+4-a,shade(jacket,-12));
        rect(x+4,middle-5,x+7,middle+3+a,shade(jacket,4));
        rect(x+4,middle+2+a,x+7,middle+4+a,0xBE8F79);
        rect(x-3,middle-12,x+5,middle-6,0xCEA18A);
        rect(x-4,middle-15,x+4,middle-11,0x272737);
        rect(x-4,middle-12,x-2,middle-8,0x272737);
        rect(x+4,middle-11,x+6,middle-9,0xB78776);
        rect(x-3,middle-9,x+1,middle-7,0xA77A70);
        if (jacket == 0x687C57) {
            rect(x-6,middle+2,x-1,middle+8,0xAA7651);
            rect(x-5,middle+3,x-2,middle+4,0xE0A673);
            for (int i=0;i<6;i++) rect(x+2-i,middle-5+i,x+3-i,middle-4+i,0xBB8F59);
        }
    }
}
