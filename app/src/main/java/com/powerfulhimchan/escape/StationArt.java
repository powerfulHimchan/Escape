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
    private final Bitmap rooms, details, cast, steps, concourse, platform, landings, gateHall, counterHall;
    private final Bitmap walkFrames, awayFrames, runFrames, stairFrames;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private static final int SHUTTER_TOP=ShutterPass.TOP, SHUTTER_SILL=ShutterPass.SILL;
    private static final float LAST_OPEN_DROP=ShutterPass.DECISION_DROP;
    private final Path gateFrontLeft=polygon(90,173,90,128,98,120,112,119,114,126,114,173);
    private final Path gateFrontRight=polygon(149,174,149,129,155,121,168,120,171,127,171,174);
    private final Path gateFrontLeftB=polygon(341,122,354,119,363,127,363,174,347,174,341,164);
    private final Path gateFrontRightB=polygon(399,121,410,119,422,128,423,174,404,174,399,164);
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
    private boolean timing, enteringKiosk, fromStairs, opensDuringTimer;

    StationArt(Context context) {
        super(context);
        rooms = load(R.drawable.station);
        details = load(R.drawable.detail);
        cast = load(R.drawable.cast);
        steps = load(R.drawable.cast_steps);
        concourse = load(R.drawable.concourse);

        platform = load(R.drawable.empty_platform);
        landings = load(R.drawable.landings);
        gateHall = load(R.drawable.gate_hall);
        counterHall = load(R.drawable.counter_hall);
        walkFrames = load(R.drawable.motion_walk);
        awayFrames = load(R.drawable.motion_away);
        runFrames = load(R.drawable.motion_run);
        stairFrames = load(R.drawable.motion_stair);
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
        enteringKiosk = false;
        departing = arriving = emptyPlatform = atShutter = counterChoice = timedScene = callLight = false;
        invalidate();
    }

    void stage(boolean departure, boolean arrival, boolean empty, boolean timed, boolean firstShutter, boolean calling, int journalPanel, boolean opening) {
        panel = journalPanel;
        opensDuringTimer = opening;
        departing = departure;
        arriving = arrival;
        emptyPlatform = empty;
        timedScene = timed;
        atShutter = timed && setting == 1;
        counterChoice = firstShutter;
        callLight = calling;
    }

    void enterKiosk(boolean stairs) {
        enteringKiosk=true;
        fromStairs=stairs;
    }

    int actionDuration() {
        if (counterChoice) return 1750;
        return panel==10 || panel==3 && action.equals("runLeft") ? 1900 : ACTION_MS;
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

    int viewId() {
        if (panel >= 0) return 25 + panel;
        if (departing || arriving || emptyPlatform) return 24;
        return setting + (close ? 12 : 0);
    }

    void arrivalView(int previous) {
        if (previous != viewId() || departing || arriving || atShutter) return;
        if (emptyPlatform) {
            emptyPlatform = false;
            panel = 9;
        } else if (panel == 8) {
            panel = 11;
        } else if (panel < 0) {
            if (setting == 5) panel = 8;
            else if (setting == 8) panel = 10;
            else close = !close;
        }
    }

    private void fullImage(Canvas c, Bitmap image) {
        bitmap(c,image,0,0,image.getWidth(),image.getHeight(),0,0,512,256);
    }

    private void room(Canvas c) {
        if (panel == 0) { fullImage(c,gateHall); return; }
        if (panel == 1 || atShutter) { fullImage(c,counterHall); return; }
        if (panel >= 0) {
            int tile = panel < 4 ? panel : panel - 4;
            bitmap(c,panel < 4 ? concourse : landings,512*(tile%2)+2,256*(tile/2)+2,508,252,0,0,512,256);
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
        float gap = departing ? 1-ease((elapsed-850)/550) : ease((elapsed-2350)/480);
        if (gap > 0) {
            c.save();
            c.clipPath(door);
            c.drawColor(0xff242433);
            bitmap(c,rooms,201,96,15,63,201-16*gap,96,15,63);
            bitmap(c,rooms,216,93,14,63,216+16*gap,93,14,63);
            c.restore();
        }
        c.restore();
    }

    private final Path door = polygon(201,103,229,93,229,148,201,158);

    private void shutter(Canvas c, float amount) {
        // The compact roll at the lintel and the moving slats share the same material.
        float bottom=SHUTTER_TOP+(SHUTTER_SILL-SHUTTER_TOP)*amount;
        c.save();
        c.clipRect(40,SHUTTER_TOP,249,bottom);
        for (float y=bottom-13; y>SHUTTER_TOP-14; y-=13)
            bitmap(c,counterHall,64,72,306,17,40,y,209,13);
        if (amount>0) prop(c,0xff372c29,40,bottom-2,209,2);
        c.restore();
    }

    private void gateArm(Canvas c,int lane,float turn) {
        float hub=lane==0?111:363;
        paint.setStrokeWidth(1.9f);
        for (int i=0;i<3;i++) {
            double angle=i*2*Math.PI/3+turn*2*Math.PI/3;
            float tipX=hub+24+(float)Math.cos(angle)*9, tipY=134+(float)Math.sin(angle)*13;
            paint.setColor(0xff433e3c);
            c.drawLine(hub,134,tipX,tipY,paint);
            paint.setColor(0xffc9b99b);
            c.drawLine(hub,133,tipX,tipY-1,paint);
        }
        paint.setStrokeWidth(1);
    }

    private void gateHousing(Canvas c,Path housing) {
        c.save();
        c.clipPath(housing);
        fullImage(c,gateHall);
        c.restore();
    }

    private final Path officeDoor = polygon(385,81,444,87,444,185,385,176);

    private void openOfficeDoor(Canvas c, float progress) {
        float opening=ease((progress-.33f)/.42f);
        if (opening<=0) return;
        c.save();
        c.clipPath(officeDoor);
        c.drawColor(0xff29232a);
        prop(c,0xff74513c,386,168,59,18);
        c.scale(1-.84f*opening,1,385,176);
        bitmap(c,concourse,514,258,508,252,0,0,512,256);
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
        if (!close || panel >= 0 || atShutter) return;
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

    private void timedFixture(Canvas c,float amount) {
        if (setting == 7) {
            // Glass leaves slide across the open street entrance. The street remains visible through them.
            float width = 153 * amount;
            paint.setColor(0x406a8290);
            c.drawRect(269,29,269+width,182,paint);
            prop(c,0xff3a3a40,267+width,29,3,153);
            prop(c,0xff5b5350,268,114,Math.round(width),3);
        } else if (setting == 6 || setting == 11) {
            boolean interior = setting == 6;
            int x = interior ? 1 : 275, y = interior ? 39 : 72;
            int w = interior ? 101 : 40, h = interior ? 213 : 105;
            float gap = (1-amount) * w * .5f;
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
            c.translate(0,-224*(1-amount));
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

    private void sprite(Canvas c, Bitmap sheet, int columns, int row, int frame,
                        float x,float y,float height,boolean left) {
        int size=sheet.getWidth()/columns;
        c.save();
        if (left) c.scale(-1,1,x,y);
        bitmap(c,sheet,frame*size,row*size,size,size,x-height/2,y-height,height,height);
        c.restore();
    }

    private void shadow(Canvas c,float x,float y,float h) {
        paint.setColor(0x60202434);
        c.drawOval(x-h*.13f,y-2,x+h*.13f,y+1,paint);
    }

    private void person(Canvas c,int who,float x,float y,float h,String pose,float cycle,boolean left) {
        shadow(c,x,y,h);
        boolean retreat=pose.equals("backstep");
        if (pose.equals("walk") || retreat || pose.startsWith("run") || pose.startsWith("away")) {
            Bitmap sheet=pose.startsWith("run") ? runFrames : pose.startsWith("away") ? awayFrames : walkFrames;
            int frame=((int)(cycle*6)+who)%6;
            sprite(c,sheet,6,who,retreat?5-frame:frame,x,y,h,left);
            return;
        }
        boolean reach=pose.equals("reach");
        c.save();
        if (left) c.scale(-1,1,x,y);
        if (reach || pose.equals("signal"))
            c.rotate((float)Math.sin(unit(cycle)*Math.PI)*(reach?10:-6),x,y);
        sprite(c,reach?steps:cast,3,reach?2:0,who,x,y,h,false);
        c.restore();
    }

    static boolean travels(String motion) {
        return motion.startsWith("run") || motion.startsWith("away") || motion.equals("ascend")
                || motion.equals("descend") || motion.equals("disembark") || motion.equals("backstep")
                || motion.equals("left") || motion.equals("right");
    }

    private void stairPerson(Canvas c,float x,float y,float height,float along,int direction,boolean left) {
        shadow(c,x,y,height);
        int frame=(int)(along*18)%6;
        sprite(c,stairFrames,6,direction>0?0:1,frame,x,y,height,left);
    }

    private FloorPlan currentFloor() {
        return atShutter || panel==1 ? FloorPlan.SHUTTER : FloorPlan.of(viewId());
    }

    private void companion(Canvas c, int who, float x, boolean acting, String cue, float p) {
        String pose = acting ? cue : "hold";
        FloorPlan floor=currentFloor();
        if (pose.equals("backstep") && (setting==11 || setting==6 || atShutter)) {
            float t=interval(p,0,.72f);
            float y=188+10*t;
            person(c,who,x-18*t,y,floor.heightAt(y)*(who==1?.94f:.98f),pose,t*.8f,false);
            return;
        }
        boolean first=pose.endsWith("Left");
        if (setting==7 && close && panel<0) first=false;
        if (setting==6 && close && travels(pose)) first=true;
        FloorPlan.Exit exit=first?floor.first:floor.second;
        float y=188;
        // Follow through the same opening a fraction of a stride behind the leader.
        float along=travels(pose)?ease((p-.10f*who)/(1-.10f*who)):0;
        float turn=Math.min(1,along*2), through=Math.max(0,along*2-1);
        float endX=x+(exit.turnX-x)*turn+(exit.endX-exit.turnX)*through;
        float endY=y+(exit.turnY-y)*turn+(exit.endY-exit.turnY)*through;
        boolean left=along<.5f?exit.turnX<x:exit.endX<exit.turnX;
        float h=floor.heightAt(endY)*(who==1?.94f:.98f);
        float distance=turn*(float)Math.hypot(exit.turnX-x,exit.turnY-y)
                +through*(float)Math.hypot(exit.endX-exit.turnX,exit.endY-exit.turnY);
        person(c,who,endX,endY,h,pose,travels(pose)?distance/(floor.standingHeight*.75f):p,left);
    }

    private static float mix(float a,float b,float t) { return a+(b-a)*t; }

    private static float interval(float p,float start,float end) {
        return ease((p-start)/(end-start));
    }

    private boolean crossingGroup() {
        return crosses(action) || minseo && crosses(minseoAction) || jian && crosses(jianAction);
    }

    private static boolean crosses(String cue) {
        return travels(cue) && !cue.equals("backstep");
    }

    // The timer leaves a person-sized opening until the decision is resolved. On a
    // crossing, the last traveller completes their path before the door can shut.
    private float doorAmount(float p,boolean acting) {
        float selected=acting?closedAtChoice:closed;
        if (opensDuringTimer) return 1-mix(selected,1,acting?interval(p,0,.25f):0);
        float initial=selected*LAST_OPEN_DROP;
        if (!acting) return initial;
        return mix(initial,1,interval(p,crossingGroup()?.82f:0,1));
    }

    private void counterSequence(Canvas c,float p,boolean acting) {
        FloorPlan floor=currentFloor();
        float x=floor.x,y=floor.y;
        boolean running=acting && action.startsWith("run");
        boolean pickup=acting && action.equals("pick");
        float from=(acting?closedAtChoice:closed)*LAST_OPEN_DROP;
        float drop=from;
        int duck=-1;
        String pose="hold";
        float cycle=0;
        boolean left=false;
        if (running) {
            x=ShutterPass.x(p,floor.x);
            y=ShutterPass.y(p,floor.y);
            left=true;
            pose="runLeft";
            cycle=2*interval(p,0,.34f)+interval(p,.34f,.90f);
            duck=ShutterPass.duckFrame(p,y);
            drop=ShutterPass.drop(closedAtChoice,y);
        } else if (acting) {
            drop=mix(from,1,interval(p,0,.51f));
            float approach=interval(p,0,.41f);
            float toRamp=interval(p,pickup?.54f:0,pickup?.81f:.69f);
            float inside=interval(p,pickup?.81f:.69f,1);
            float counterX=pickup?mix(x,388,approach):x;
            float counterY=pickup?mix(y,187,approach):y;
            x=mix(mix(counterX,478,toRamp),459,inside);
            y=mix(mix(counterY,192,toRamp),137,inside);
            boolean reaching=pickup && p>=.37f && p<.54f;
            pose=reaching?"reach":inside>0?"awayRight":"runRight";
            cycle=reaching?unit((p-.37f)/.17f):(pickup?approach:0)+2*toRamp+inside;
            left=inside>0;
        }
        shutter(c,drop);
        if (!radio || pickup && p>=.49f) {
            // Replace the radio on the painted counter with the adjacent empty ledge.
            bitmap(c,counterHall,559,170,14,31,384,113,10,21);
        }
        boolean behind=running && y<SHUTTER_SILL-9;
        c.save();
        if (behind) c.clipRect(40,SHUTTER_TOP,249,SHUTTER_SILL);
        float h=floor.heightAt(y);
        if (duck>=0) {
            shadow(c,x,y,h);
            sprite(c,stairFrames,6,2,duck,x,y,h,left);
        } else person(c,0,x,y,h,pose,cycle,left);
        c.restore();
        if (behind) shutter(c,drop);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        long now=SystemClock.uptimeMillis();
        float elapsed=now-entered;
        float p=started<0 ? 0 : unit((now-started)/(float)actionDuration());
        boolean acting=started>=0;
        c.save();
        c.scale(getWidth()/512f,getHeight()/256f);
        room(c);
        fixtures(c, now, p);
        if (panel==3 && acting && action.equals("runRight")) openOfficeDoor(c,p);
        if(departing || arriving) train(c,elapsed);
        float door=doorAmount(p,acting);
        boolean trainPass = acting && !timedScene && close && panel<0
                && (setting==6 || setting==11) && crossingGroup();
        if (trainPass) {
            door=1-interval(p,0,.26f)+interval(p,.84f,1);
            timedFixture(c,door);
        }
        if (timedScene && !atShutter && !emptyPlatform) {
            if (close && panel < 0) timedFixture(c,door);
            else c.drawColor(((int)(closed*47) << 24) | 0x000a1020);
        }
        if((callLight && panel < 4) || atShutter) {
            boolean on=((int)elapsed/460)%2==0;
            if(atShutter) prop(c,on?0xffff9d54:0xffc94428,273,66,4,5);
            else if(setting==2) prop(c,on?0xffffaa68:0xff87392c,close?410:300,close?86:89,4,3);
            else if(panel==3) prop(c,on?0xffff8546:0xffab371f,455,106,6,6);
        }
        if (atShutter && !counterChoice) shutter(c,door);
        if (counterChoice) {
            counterSequence(c,p,acting);
            c.restore();
            if (acting && p<1 || timing) postInvalidateOnAnimation();
            return;
        }
        // Leave the tail of the action for door motion after the last foot is through.
        float travelTime = (timedScene || trainPass) && acting && crossingGroup() ? unit(p/.82f) : p;
        FloorPlan floor = currentFloor();
        float x=floor.x, y=floor.y, walk=p;
        String pose="hold";
        boolean left=false;
        int stair=0;
        if(departing) {
            float step=ease(elapsed/780);
            x=219+22*step; y=148+31*step;
            pose=elapsed<780?"walk":elapsed<1660?"reach":"hold"; walk=elapsed<780 ? step*1.25f : unit(elapsed/1660);
        }
        if (enteringKiosk && !acting && elapsed<860) {
            float t=ease(elapsed/860);
            float entryX=fromStairs?96:432;
            x=entryX+(x-entryX)*t;
            y=167+(y-167)*t;
            pose="walk";
            left=!fromStairs;
            walk=t*1.8f;
        }
        if(acting) {
            boolean first = action.endsWith("Left") || action.equals("left") || action.equals("ascend");
            // A single doorway serves both choices at the street and train thresholds.
            if ((setting==7 && close || setting==11) && panel < 0) first = setting==11 && !close;
            if (setting==6 && (trainPass || action.equals("disembark") || action.equals("awayLeft"))) first=true;
            if (panel==2 || panel==4) first=action.equals("pick");
            if (action.equals("backstep")) first=false;
            FloorPlan.Exit exit=first ? floor.first : floor.second;
            boolean fixture=panel==2;
            boolean travel=travels(action) || fixture || panel==4 && action.equals("pick");
            pose=action;
            if (action.equals("backstep") && (setting==11 || setting==6 || atShutter)) {
                float retreat=interval(p,0,.72f);
                x-=18*retreat;
                y+=10*retreat;
                walk=retreat*.8f;
            } else if(travel) {
                float t=ease(unit(travelTime/(fixture ? .74f : 1f)));
                float length1=(float)Math.hypot(exit.turnX-x,exit.turnY-y);
                float length2=(float)Math.hypot(exit.endX-exit.turnX,exit.endY-exit.turnY);
                float corner=length1/(length1+length2);
                boolean last=t>=corner;
                float f=last ? (t-corner)/(1-corner) : t/corner;
                float ax=last?exit.turnX:x, ay=last?exit.turnY:y;
                float bx=last?exit.endX:exit.turnX, by=last?exit.endY:exit.turnY;
                left=bx<ax;
                x=ax+(bx-ax)*f;
                stair=last ? exit.stairs : 0;
                float strides=f*6;
                float lift=stair==0 ? f : ((int)strides+ease(strides-(int)strides))/6f;
                y=ay+(by-ay)*lift;
                walk=t*(length1+length2)/(floor.standingHeight*(action.startsWith("run")?.92f:.75f));
                if (stair!=0) walk=f;
                if (action.equals("backstep")) left=!left;
                if (fixture && p>.72f || panel==4 && action.equals("pick") && p>.85f) pose="reach";
                else if (action.equals("backstep")) pose="backstep";
                else if (action.startsWith("run")) pose=action;
                else pose=by<ay-7?"awayRight":"walk";
            } else if(action.equals("pick")) pose="reach";
        }
        if (panel==0) {
            int lane=action.endsWith("Left")?0:1;
            if (!acting || lane!=0 || y>=179) gateArm(c,0,0);
            if (!acting || lane!=1 || y>=179) gateArm(c,1,0);
        }
        if(minseo) companion(c,1,setting==5?172:309,acting,minseoAction,travelTime);
        if(jian) companion(c,2,164,acting,jianAction,travelTime);
        if(lamp && setting==5){c.save();c.translate(x,y);paint.setColor(0x58ffe1a1);c.drawPath(beam,paint);c.restore();}
        if (hero) {
            float h=floor.heightAt(y);
            if (stair == 0) person(c,0,x,y,h,pose,walk,left);
            else stairPerson(c,x,y,h,walk,stair,left);
        }
        if (panel==0 && acting) {
            int lane=action.endsWith("Left")?0:1;
            if (y<179) {
                gateHousing(c,lane==0?gateFrontLeft:gateFrontLeftB);
                gateHousing(c,lane==0?gateFrontRight:gateFrontRightB);
                gateArm(c,lane,interval(183-y,0,22));
            }
        }
        if ((timedScene || trainPass) && close && panel<0 && acting && crossingGroup() && p>=.82f) {
            if (atShutter) shutter(c,door);
            else if (setting==6 || setting==7 || setting==11) timedFixture(c,door);
        }
        c.restore();
        if ((acting && p<1) || enteringKiosk && elapsed<860 || timing || (departing && elapsed<4100) || (arriving && elapsed<2900))
            postInvalidateOnAnimation();
        else if(close && setting==9) postInvalidateDelayed(80);
        else if(callLight || atShutter || close && (setting==2 || setting==5)) postInvalidateDelayed(420);
    }
}
