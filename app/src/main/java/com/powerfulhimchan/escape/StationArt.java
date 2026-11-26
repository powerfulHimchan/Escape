package com.powerfulhimchan.escape;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Draws a room and its moving occupants in the same 512-by-256 composition. */
final class StationArt extends View {
    private static final int FRAME_MS = 820;
    private final Bitmap rooms;
    private final Bitmap details;
    private final Bitmap cast;
    private final Paint paint = new Paint();
    private final Rect crop = new Rect();
    private final RectF dest = new RectF();
    private final Path shape = new Path();
    private int setting;
    private boolean close, minseo, jian, radio, lamp, hero = true;
    private float shutter = 1f;
    private boolean timer;
    private String motion = "hold";
    private long started = -10000;

    StationArt(Context context) {
        super(context);
        rooms = BitmapFactory.decodeResource(getResources(), R.drawable.station);
        details = BitmapFactory.decodeResource(getResources(), R.drawable.detail);
        cast = BitmapFactory.decodeResource(getResources(), R.drawable.cast);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void configure(int scene, boolean closeUp, boolean withMinseo, boolean withJian,
                   boolean showRadio, boolean light, boolean showHero) {
        setting = scene;
        close = closeUp || scene == 5;
        minseo = withMinseo;
        jian = withJian;
        radio = showRadio;
        lamp = light;
        hero = showHero;
        started = -10000;
        invalidate();
    }

    void countdown(long remaining, long total, boolean running) {
        timer = running;
        if (remaining >= 0) shutter = Math.max(0f, Math.min(1f, remaining / (float) total));
        else shutter = 1f;
        invalidate();
    }

    void play(String action) {
        motion = action;
        started = SystemClock.uptimeMillis();
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long now = SystemClock.uptimeMillis();
        float progress = Math.min(1f, (now - started) / (float) FRAME_MS);
        boolean moving = progress < 1f && !"hold".equals(motion);
        canvas.save();
        canvas.scale(getWidth() / 512f, getHeight() / 256f);
        int tile = !close && setting == 4 ? 5 : setting;
        Bitmap sheet = close ? details : rooms;
        int left = (tile % 3) * 512, top = (tile / 3) * 256;
        crop.set(left+2,top+2,left+510,top+254);
        dest.set(0,0,512,256);
        paint.setColor(0xffffffff);
        canvas.drawBitmap(sheet,crop,dest,paint);

        if (setting == 1 && close) {
            drawShutter(canvas);
            if (radio && !(moving && "pick".equals(motion) && progress > .68f)) {
                paint.setColor(0xff262837);
                canvas.drawRect(402,127,412,139,paint);
                canvas.drawRect(409,121,411,128,paint);
                paint.setColor(0xffffb279);
                canvas.drawRect(404,130,409,132,paint);
            }
        }

        float x = setting == 5 ? 128 : 214, y = setting == 5 ? 210 : 219;
        if (setting == 6) x = 245;
        if (minseo) person(canvas,1,setting == 5 ? 172 : 309,188,69,false,false);
        if (jian) person(canvas,2,164,188,64,false,false);
        float eased = progress * progress * (3f - 2f * progress);
        boolean leftward = "left".equals(motion) || "runLeft".equals(motion);
        if (moving) {
            float targetX = leftward ? 133 : 323;
            if (setting == 1) targetX = "pick".equals(motion) ? 395 : 162;
            if (setting == 5) targetX = leftward ? 105 : 183;
            x += (targetX-x)*eased;
            y -= (setting == 1 ? 50 : 26)*eased;
        }
        if (lamp && setting == 5) {
            paint.setColor(0x58ffe1a1);
            shape.rewind(); shape.moveTo(x+8,y-30); shape.lineTo(x+90,y-28);
            shape.lineTo(x+57,y-5); shape.close(); canvas.drawPath(shape,paint);
        }
        if (hero) {
            float height = 79;
            if (moving && ("pick".equals(motion) || setting == 1 && motion.startsWith("run")))
                height -= 15 * Math.sin(progress * Math.PI);
            person(canvas,0,x,y,height,moving && ((now-started)/137)%2==1,moving && leftward);
        }
        canvas.restore();
        if (moving || timer) postInvalidateDelayed(32);
    }

    private void drawShutter(Canvas canvas) {
        // Continue the slats down the lit opening in the close view.
        float drop = (1f-shutter)*42f;
        if (drop < 1f) return;
        canvas.save();
        shape.rewind();
        shape.moveTo(79,128); shape.lineTo(277,110);
        shape.lineTo(277,110+drop); shape.lineTo(79,128+drop); shape.close();
        canvas.clipPath(shape);
        canvas.drawColor(0xff4f5060);
        paint.setColor(0xff292b38);
        for(int y=110;y<190;y+=5) canvas.drawLine(78,y+18,278,y,paint);
        canvas.restore();
    }

    private void person(Canvas canvas,int col,float x,float y,float h,boolean stride,boolean flipped) {
        int w=cast.getWidth()/3, row=cast.getHeight()/2;
        int t=stride ? row : 0;
        crop.set(col*w,t,(col+1)*w,t+row);
        paint.setColor(0x60202434);
        canvas.drawOval(x-14,y-4,x+15,y+3,paint);
        paint.setColor(0xffffffff);
        dest.set(x-h*.5f,y-h,x+h*.5f,y);
        canvas.save();
        if(flipped) canvas.scale(-1,1,x,y);
        canvas.drawBitmap(cast,crop,dest,paint);
        canvas.restore();
    }
}
