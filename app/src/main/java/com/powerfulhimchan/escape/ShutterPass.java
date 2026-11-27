package com.powerfulhimchan.escape;

/** Geometry of the first shutter, in stage coordinates. The trailing foot clears at y=169. */
final class ShutterPass {
    static final int TOP=65, SILL=185;
    static final float DECISION_DROP=.41f;

    private static float progress(float value,float start,float end) {
        float t=Math.max(0,Math.min(1,(value-start)/(end-start)));
        return t*t*(3-2*t);
    }
    private static float mix(float a,float b,float t) { return a+(b-a)*t; }

    static float x(float p,float start) {
        return mix(mix(start,164,progress(p,0,.34f)),147,progress(p,.34f,.90f));
    }

    static float y(float p,float start) {
        return mix(mix(start,201,progress(p,0,.34f)),142,progress(p,.34f,.90f));
    }

    static int duckFrame(float p,float footY) {
        if (p<.19f) return -1;
        if (p<.37f) return Math.min(3,(int)(4*(p-.19f)/.18f));
        if (footY>SILL-16) return 3;
        return footY>153?4:5;
    }

    static float drop(float countdownFraction,float footY) {
        return mix(countdownFraction*DECISION_DROP,1,progress(SILL-footY,16,42));
    }
}
