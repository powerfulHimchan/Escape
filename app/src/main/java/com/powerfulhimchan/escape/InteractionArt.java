package com.powerfulhimchan.escape;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Region;
import org.json.JSONObject;

/** Item grips use room pixels. Gestures use coordinates on the 256px standing portrait. */
final class InteractionArt {
    static final class Pickup {
        final String item, surface;
        final float x,y,feetX,feetY;
        Pickup(JSONObject data) {
            item=data.optString("item"); surface=data.optString("surface");
            x=data.optInt("x"); y=data.optInt("y");
            feetX=data.optInt("footX"); feetY=data.optInt("footY");
        }
        boolean low() { return surface.equals("floor") || surface.equals("low"); }
    }
    private final Paint paint=new Paint();
    private final Rect portrait=new Rect(0,0,256,256);
    private final Path forearm=new Path();
    InteractionArt() {
        forearm.moveTo(85,137); forearm.lineTo(116,140); forearm.lineTo(108,158);
        forearm.lineTo(112,171); forearm.lineTo(109,185); forearm.lineTo(89,183); forearm.close();
    }
    private void box(Canvas c,int color,float x,float y,float w,float h) {
        paint.setColor(color); c.drawRect(x,y,x+w,y+h,paint);
    }
    void bin(Canvas c,Pickup s) {
        box(c,0xff352f2e,s.x-10,s.y-11,20,18);
        box(c,0xff756957,s.x-10,s.y+4,20,3);
        box(c,0xff93836b,s.x-10,s.y-11,2,16);
    }
    void item(Canvas c,String item,float x,float y) {
        switch(item) {
            case "map":
                box(c,0xff665548,x-7,y-3,15,8); box(c,0xffecd8ad,x-7,y-4,14,7);
                box(c,0xff92836c,x-1,y-3,1,6); box(c,0xff7e9085,x+1,y-2,4,1); break;
            case "radio":
                box(c,0xff29303a,x-4,y-8,8,17); box(c,0xff41434a,x-3,y-14,2,7);
                box(c,0xff93a79a,x-2,y-5,4,3); box(c,0xff887961,x-2,y+2,4,1); break;
            case "battery":
                box(c,0xff393b41,x-4,y-2,9,5); box(c,0xffd7ae63,x-4,y-2,3,4);
                box(c,0xffc7c8b8,x+5,y-1,2,2); break;
            case "lamp":
                box(c,0xff394049,x-7,y-2,12,5); box(c,0xff767a76,x+3,y-4,4,8);
                box(c,0xffe4d1a0,x+7,y-3,2,6); break;
        }
    }
    private void arm(Canvas c,Bitmap cast,float handX,float handY) {
        c.save(); c.clipPath(forearm,Region.Op.DIFFERENCE);
        paint.setColor(-1); c.drawBitmap(cast,portrait,portrait,paint); c.restore();
        float dx=handX-99,dy=handY-141;
        c.save(); c.translate(99,141);
        c.rotate((float)Math.toDegrees(Math.atan2(dy,dx)-Math.atan2(35,2)));
        c.scale(1,(float)Math.hypot(dx,dy)/35.06f);
        c.translate(-99,-141); c.clipPath(forearm);
        c.drawBitmap(cast,portrait,portrait,paint); c.restore();
    }
    void stand(Canvas c,Bitmap cast,float x,float y,float h,boolean left,float gx,float gy) {
        float scale=h/256f,direction=left?-1:1;
        c.save(); c.translate(x,y); c.scale(scale*direction,scale); c.translate(-128,-256);
        arm(c,cast,128+(gx-x)/(direction*scale),256+(gy-y)/scale); c.restore();
    }
    private static final int[] ROUTE={142,151,150,151,150,142,166,142};
    private static final int[] TRIANGLE={144,152,154,140,165,152,144,152};
    private static final int[] WORDS={142,145,146,142,148,146,151,142,154,146,157,142,160,146,166,144};
    private static float clamp(float t) { return Math.max(0,Math.min(1,t)); }
    boolean gesture(Canvas c,Bitmap cast,String cue,float x,float y,float h,boolean left,float p) {
        boolean phone=cue.equals("phoneSketch")||cue.equals("phoneNote");
        boolean note=phone||cue.equals("sketch")||cue.equals("triangle")||cue.equals("write")||cue.equals("writeTime");
        if (!note && !cue.equals("knock") && !cue.equals("inspect") && !cue.equals("file")
                && !cue.equals("insert") && !cue.equals("cup")) return false;
        float t=clamp((p-.18f)/.60f),lift=clamp(p/.18f)*clamp((1-p)/.2f);
        int[] path=cue.equals("sketch")?ROUTE:cue.equals("triangle")||cue.equals("phoneSketch")?TRIANGLE:WORDS;
        int segments=path.length/2-1;
        c.save(); c.translate(x,y); c.scale((left?-h:h)/256f,h/256f); c.translate(-128,-256);
        float tipX=149,tipY=141;
        if (note) {
            int i=Math.min(segments-1,(int)(segments*t))*2;
            float f=clamp(segments*t-i/2);
            tipX=path[i]+(path[i+2]-path[i])*f; tipY=path[i+1]+(path[i+3]-path[i+1])*f;
            if(cue.equals("writeTime")) { tipX=140+29*t; tipY=149; }
        } else if(cue.equals("knock")) tipX+=17*(float)Math.pow(Math.sin(2*Math.PI*t),2);
        else if(cue.equals("file")) { tipX-=43*t; tipY+=32*t; }
        else if(cue.equals("cup")) tipY=116+32*t;
        else if(cue.equals("insert")) tipX+=15*t;
        else tipY+=9*t;
        float handX=101+(tipX-(note&&!phone?5:0)-101)*lift,handY=176+(tipY-176)*lift;
        arm(c,cast,handX,handY);
        if(note) {
            box(c,phone?0xff27323b:0xff7b6b53,137,135,36,24);
            box(c,phone?0xffb5d2cd:0xffe2d0a7,139,137,32,20);
            paint.setColor(0xff37434a); paint.setStrokeWidth(1.8f);
            if(cue.equals("writeTime")) {
                paint.setTextSize(11); c.save(); c.clipRect(139,137,140+29*t,157);
                c.drawText("00:11",140,150,paint); c.restore();
            } else for(int j=0;j<segments;j++) {
                float f=clamp(t*segments-j); int k=j*2;
                if(f==0) break;
                c.drawLine(path[k],path[k+1],path[k]+(path[k+2]-path[k])*f,path[k+1]+(path[k+3]-path[k+1])*f,paint);
            }
            if(lift>.98f && !phone) c.drawLine(handX-2,handY-5,tipX,tipY,paint);
            box(c,0xffc19470,handX-2,handY-1,4,3);
        } else if(cue.equals("file") && t<1) box(c,0xffd4c5a7,handX,handY-8,13,9);
        else if(cue.equals("insert") && t<.85f) item(c,"battery",handX,handY);
        else if(cue.equals("cup") && t<.5f) box(c,0xff354957,handX,handY-9,9,12);
        c.restore(); return true;
    }
}
