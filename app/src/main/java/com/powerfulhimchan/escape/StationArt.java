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

/** All actors, props and backgrounds use the same 512 x 256 stage. */
final class StationArt extends View {
    static final int ACTION_MS = 1250;
    private final Bitmap rooms, details, cast, poses, steps, concourse, platform;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private final Path shutterOpening = polygon(67,126,276,105,276,174,67,199);
    private final Path trainOutline = polygon(60,130,65,121,124,98,132,91,160,82,172,83,
            188,77,191,70,220,59,232,59,260,49,273,49,289,43,303,49,322,62,
            322,129,116,202,102,202,62,186);
    private final Path trainBay = polygon(33,101,267,6,325,31,325,135,115,215,33,194);
    private final Path beam = polygon(8,-30,94,-27,58,-5);
    private String action = "hold";
    private String minseoAction = "hold", jianAction = "hold";
    private int setting;
    private int panel;
    private boolean close, minseo, jian, radio, lamp, hero;
    private boolean departing, arriving, emptyPlatform, atShutter, counterChoice, timedScene, callLight;
    private long entered, started = -1;
    private float closed, closedAtChoice;
    private boolean timing;

    StationArt(Context context) {
        super(context);
        rooms = load(R.drawable.station);
        details = load(R.drawable.detail);
        cast = load(R.drawable.cast);
        steps = load(R.drawable.cast_steps);
        concourse = load(R.drawable.concourse);
        poses = load(R.drawable.cast_motion);
        platform = load(R.drawable.empty_platform);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private Bitmap load(int res) {
        return BitmapFactory.decodeResource(getResources(), res);
    }

    private static Path polygon(int... points) {
        Path p = new Path();
        p.moveTo(points[0], points[1]);
        for (int i=2; i<points.length; i+=2) p.lineTo(points[i], points[i+1]);
        p.close();
        return p;
    }

    void configure(int scene, boolean closeUp, boolean withMinseo, boolean withJian,
                   boolean showRadio, boolean light, boolean showHero) {
        setting = scene;
        panel = -1;
        close = closeUp || scene == 5;
        minseo = withMinseo;
        jian = withJian;
        radio = showRadio;
        lamp = light;
        hero = showHero;
        entered = SystemClock.uptimeMillis();
        started = -1;
        departing = arriving = emptyPlatform = atShutter = counterChoice = timedScene = callLight = false;
        invalidate();
    }

    void stage(boolean departure, boolean arrival, boolean empty, boolean timed, boolean firstShutter, boolean calling, int journalPanel) {
        panel = journalPanel;
        departing = departure;
        arriving = arrival;
        emptyPlatform = empty;
        timedScene = timed;
        atShutter = timed && setting == 1;
        counterChoice = firstShutter;
        callLight = calling;
    }

    void countdown(long remaining, long total, boolean running) {
        timing = running;
        closed = remaining < 0 ? 0 : 1 - unit(remaining / (float) Math.max(1,total));
        invalidate();
    }

    void play(String motion, String companion, String rider) {
        minseoAction = companion;
        jianAction = rider;
        action = motion;
        closedAtChoice = closed;
        started = SystemClock.uptimeMillis();
        invalidate();
    }

    private static float unit(float p) { return Math.max(0,Math.min(1,p)); }
    private static float ease(float p) { p=unit(p); return p*p*(3-2*p); }

    private void bitmap(Canvas c, Bitmap b, int l, int t, int w, int h,
                        float x, float y, float dw, float dh) {
        source.set(l,t,l+w,t+h);
        target.set(x,y,x+dw,y+dh);
        paint.setColor(0xffffffff);
        c.drawBitmap(b,source,target,paint);
    }

    private void room(Canvas c) {
        if (panel >= 0) {
            bitmap(c,concourse,512*(panel%2)+2,256*(panel/2)+2,508,252,0,0,512,256);
            return;
        }
        if (departing || arriving || emptyPlatform) {
            bitmap(c,platform,0,0,512,256,0,0,512,256);
            return;
        }
        int tile = !close && setting == 4 ? 5 : setting;
        bitmap(c,close ? details : rooms,(tile%3)*512+2,(tile/3)*256+2,508,252,0,0,512,256);
    }

    private void train(Canvas c, float elapsed) {
        float travel = departing ? ease((elapsed-1660)/2360) : 1-ease(elapsed/2350);
        if (travel >= 1) return;
        c.save();
        c.clipPath(trainBay);
        c.translate(-306*travel,110*travel);
        c.clipPath(trainOutline);
        bitmap(c,rooms,0,0,512,256,0,0,512,256);
        float gap = departing ? 1-ease((elapsed-710)/550) : ease((elapsed-2350)/480);
        if (gap > 0) {
            c.save();
            c.clipPath(door);
            c.drawColor(0xff242433);
            bitmap(c,rooms,201,96,15,63,201-11*gap,96,15,63);
            bitmap(c,rooms,216,93,14,63,216+11*gap,93,14,63);
            c.restore();
        }
        c.restore();
    }

    private final Path door = polygon(201,103,229,93,229,148,201,158);

    private void shutter(Canvas c, float amount) {
        if (amount <= 0) return;
        c.save();
        c.clipPath(shutterOpening);
        // Repeat the painted metal slats from the original door over the opening.
        int drop = Math.round(75*amount);
        c.translate(0,drop);
        bitmap(c,details,512+66,32,212,96,66,32,212,96);
        c.restore();
    }

    private final Path supplyMap = polygon(251,77,350,55,350,131,251,154);

    private void fixtures(Canvas c, long now, float progress) {
        if (panel == 2) {
            float glow = started >= 0 && action.equals("reach") ? ease((progress-.55f)/.25f) : 0;
            c.save();
            c.clipPath(supplyMap);
            c.drawColor(((int)(195*(1-glow)) << 24) | 0x0013171b);
            c.restore();
        }
        if (!close || panel >= 0) return;
        if (setting == 2) {
            // Move the second hand on the clock above the dispatch console.
            double radians=(now-entered)/60000d*Math.PI*2;
            paint.setColor(0xff613728);
            c.drawLine(466,31,466+(float)Math.sin(radians)*12,31-(float)Math.cos(radians)*12,paint);
        } else if (setting == 5) {
            int signal=((now-entered)/680)%2==0 ? 0xffff5842 : 0xff9c3832;
            prop(c,signal,133,52,5,7);
            prop(c,signal,458,51,4,7);
        } else if (setting == 9) {
            // Fine rain crossing the shelter roof and reflections on the pavement.
            paint.setColor(0x748babc5);
            for(int i=0;i<18;i++) {
                float x=(i*113)%494+7;
                float y=(i*37+(now-entered)*.12f)%246;
                c.drawLine(x,y,x-1,y+4,paint);
            }
        }
    }

    private void timedFixture(Canvas c) {
        if (setting == 7) {
            // Glass leaves slide across the open street entrance. The street remains visible through them.
            float width = 153 * closed;
            paint.setColor(0x406a8290);
            c.drawRect(269,29,269+width,182,paint);
            prop(c,0xff3a3a40,267+width,29,3,153);
            prop(c,0xff5b5350,268,114,Math.round(width),3);
        } else if (setting == 6 || setting == 11) {
            boolean interior = setting == 6;
            int x = interior ? 1 : 275, y = interior ? 39 : 72;
            int w = interior ? 101 : 40, h = interior ? 213 : 105;
            float gap = (interior ? closed : 1-closed) * w * .28f;
            int tileX=(setting%3)*512, tileY=(setting/3)*256;
            c.save();
            c.clipRect(x,y,x+w,y+h);
            bitmap(c,details,interior ? 58 : 16,interior ? 133 : 24,85,73,x,y,w,h);
            bitmap(c,details,tileX+x,tileY+y,w/2,h,x-gap,y,w/2,h);
            bitmap(c,details,tileX+x+w/2,tileY+y,w-w/2,h,x+w/2+gap,y,w-w/2,h);
            c.restore();
        } else if (setting == 4) {
            // The shutter on the other side cuts off the view through the glass.
            c.save();
            c.clipPath(glass);
            c.translate(0,-224*(1-closed));
            paint.setColor(0xa5484650);
            c.drawRect(48,-84,370,230,paint);
            paint.setColor(0x80545259);
            for (int y=-85;y<=230;y+=8) c.drawLine(48,y,370,y-90,paint);
            c.restore();
        } else if (setting==2 || setting==3 || setting==5 || setting==8 || setting==10) {
            int alpha = (int)(closed * (65+23*Math.cos(closed*30)));
            c.drawColor((alpha<<24) | 0x000a0e21);
        }
    }

    private final Path glass = polygon(53,1,366,48,366,143,53,251);

    private void prop(Canvas c, int color, float x, float y, int w, int h) {
        paint.setColor(color);
        c.drawRect(x,y,x+w,y+h,paint);
    }

    private void person(Canvas c,int who,float x,float y,float height,String move, float walk,boolean flip) {
        boolean back = move.startsWith("away");
        boolean sprint = move.startsWith("run");
        boolean step = sprint || back || move.equals("backstep") || move.equals("walk") || move.equals("pick");
        int phase = (int)(walk*(sprint ? 8 : 6)+who)%4;
        Bitmap sheet = back || sprint ? poses : cast;
        int size=sheet.getWidth()/3;
        int row=0;
        if (back) row=phase/2;
        else if (sprint) row=2+phase/2;
        else if (move.equals("reach")) { sheet=steps; row=2; }
        else if (step) {
            if (phase==0) row=1;
            else { sheet=steps; row=phase==2?0:1; }
        }
        size=sheet.getWidth()/3;
        float bob = step ? (float)Math.abs(Math.sin(walk*6*Math.PI+who))*(sprint?3:1.4f) : 0;
        paint.setColor(0x60202434);
        c.drawOval(x-height*.15f,y-3,x+height*.15f,y+2,paint);
        c.save();
        if(flip) c.scale(-1,1,x,y);
        if (move.equals("reach") || move.equals("signal"))
            c.rotate((float)Math.sin(walk*Math.PI)*(move.equals("reach")?12:-6),x,y);
        bitmap(c,sheet,who*size,row*size,size,size,x-height*.5f,y-height-bob,height,height);
        c.restore();
    }

    private void companion(Canvas c, int who, float x, boolean acting, String cue, float p) {
        String pose = acting ? cue : "hold";
        boolean left = pose.endsWith("Left");
        boolean travels = pose.startsWith("run") || pose.startsWith("away") || pose.equals("backstep");
        float along = travels ? ease((p-.08f*who)/(1-.08f*who)) : 0;
        float dx = pose.equals("backstep") ? -18 : left ? -35 : 35;
        person(c,who,x+dx*along,188-13*along,who==1?69:64,pose,p,left);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now=SystemClock.uptimeMillis();
        float elapsed=now-entered;
        float p=started<0 ? 0 : unit((now-started)/(float)ACTION_MS);
        boolean acting=started>=0;
        c.save();
        c.scale(getWidth()/512f,getHeight()/256f);
        room(c);
        fixtures(c, now, p);
        if(departing || arriving) train(c,elapsed);
        if(timedScene && !atShutter && !emptyPlatform) timedFixture(c);
        if(callLight || atShutter) {
            boolean on=((int)elapsed/460)%2==0;
            if(atShutter) prop(c,on?0xffff6635:0xffad301f,312,48,7,10);
            else if(setting==2) prop(c,on?0xffffaa68:0xff87392c,close?410:300,close?86:89,4,3);
            else if(panel==3) prop(c,on?0xffff8546:0xffab371f,455,106,6,6);
        }
        float drop=closed;
        if(atShutter && acting) {
            float finish=action.startsWith("run") ? ease((p-.50f)/.5f) : ease(p/.9f);
            drop=closedAtChoice+(1-closedAtChoice)*finish;
        }
        if(atShutter) {
            shutter(c,drop);
            if(radio && !(acting && action.equals("pick") && p>.67f)) {
                prop(c,0xff232532,402,127,10,12);
                prop(c,0xff232532,409,121,2,8);
                prop(c,0xffffb279,404,130,5,2);
            }
        }
        float x=setting==5?128:214, y=setting==5?210:213;
        float h=78, walk=p;
        String pose="hold";
        boolean left=false;
        if(departing) {
            float step=ease(elapsed/780);
            x=219+22*step; y=148+31*step; h=58+9*step;
            pose=elapsed<780?"walk":elapsed<1660?"reach":"awayRight"; walk=unit(elapsed/1660);
        }
        if(acting) {
            pose=action;
            left=action.endsWith("Left") || action.equals("left");
            if(action.equals("left") || action.equals("right")) pose="walk";
            float destX=left?145:302, destY=y-27;
            if(action.equals("backstep")) { destX=x-33; destY=y-17; }
            if(action.equals("signal") || action.equals("reach") || action.equals("hold")) {destX=x;destY=y;}
            if(panel==0) {destX=left?130:393;destY=left?189:151;}
            if(panel==2) {left=action.equals("pick"); destX=left?160:307;destY=185;if(p>.62f)pose="reach";}
            if(counterChoice) {
                if(action.equals("pick")) {destX=397;destY=172; if(p>.65f)pose="reach";}
                else if(action.startsWith("run")){destX=155;destY=144;h-=17*ease(p/.65f);}
                else {destX=322;destY=194;pose="backstep";}
            }
            float along = ease(atShutter && action.startsWith("run") ? p/.65f : p);
            x+=(destX-x)*along; y+=(destY-y)*along;
        }
        if(minseo) companion(c,1,setting==5?172:309,acting,minseoAction,p);
        if(jian) companion(c,2,164,acting,jianAction,p);
        if(lamp && setting==5){c.save();c.translate(x,y);paint.setColor(0x58ffe1a1);c.drawPath(beam,paint);c.restore();}
        if(hero) person(c,0,x,y,h,pose,walk,left);
        if(atShutter && acting && action.startsWith("run") && p>.5f) shutter(c,drop);
        c.restore();
        if ((acting && p<1) || timing || (departing && elapsed<4100) || (arriving && elapsed<2900))
            postInvalidateDelayed(35);
        else if(close && setting==9) postInvalidateDelayed(80);
        else if(callLight || atShutter || close && (setting==2 || setting==5)) postInvalidateDelayed(420);
    }
}
