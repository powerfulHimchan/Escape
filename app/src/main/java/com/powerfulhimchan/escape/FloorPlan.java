package com.powerfulhimchan.escape;

/** Foot positions in the painted 512 x 256 rooms. Each exit has an approach and a threshold. */
final class FloorPlan {
    final int x, y, standingHeight;
    final float depthScale;
    final Exit first, second;

    static final class Exit {
        final int turnX, turnY, endX, endY, stairs;
        Exit(int ax, int ay, int bx, int by, int stairs) {
            turnX=ax; turnY=ay; endX=bx; endY=by; this.stairs=stairs;
        }
    }

    private static Exit via(int x,int y,int endX,int endY) { return new Exit(x,y,endX,endY,0); }
    private static Exit up(int x,int y,int endX,int endY) { return new Exit(x,y,endX,endY,1); }
    private static Exit down(int x,int y,int endX,int endY) { return new Exit(x,y,endX,endY,-1); }
    private FloorPlan(int x,int y,int standingHeight,float depthScale,Exit first,Exit second) {
        this.x=x; this.y=y; this.standingHeight=standingHeight; this.depthScale=depthScale; this.first=first; this.second=second;
    }
    private static FloorPlan floor(int x,int y,int height,float depth,Exit a,Exit b) {
        return new FloorPlan(x,y,height,depth,a,b);
    }

    // Heights are calibrated at the marked floor position against a door, gate or rail.
    float heightAt(float footY) {
        return Math.max(standingHeight*.46f, standingHeight+(footY-y)*depthScale);
    }

    // Wide room atlas, then detail atlas, empty platform and the illustrated story panels.
    static final FloorPlan SHUTTER=floor(271,218,137,.60f,via(164,201,147,142),via(478,192,459,137));
    private static final FloorPlan[] ROOMS = {
        floor(278,204,76,0.32f,via(237,180,216,151),via(344,150,403,112)),
        floor(269,203,84,0.35f,via(213,171,166,151),via(344,165,426,167)),
        floor(273,198,86,0.36f,via(176,176,107,167),via(337,151,405,152)),
        floor(250,198,88,0.38f,via(159,173,82,175),via(314,140,350,105)),
        floor(291,218,88,0.46f,via(143,182,220,124),via(381,165,418,119)),
        floor(134,201,95,0.48f,via(119,180,146,154),via(171,168,204,128)),
        floor(241,200,88,0.4f,via(142,184,79,173),via(268,146,282,114)),
        floor(251,208,92,0.36f,up(235,180,242,49),up(284,180,277,52)),
        floor(328,210,86,0.32f,up(322,181,219,113),up(345,183,241,116)),
        floor(222,213,88,0.42f,via(164,181,93,174),via(316,177,401,156)),
        floor(245,202,88,0.36f,via(169,176,124,164),via(336,159,393,152)),
        floor(278,204,76,0.32f,via(237,180,216,151),via(344,150,403,112)),
        floor(246,223,144,0.62f,via(256,185,273,149),via(395,159,438,109)),
        floor(271,218,137,0.6f,via(207,193,145,150),via(339,192,397,172)),
        floor(241,212,155,0.62f,via(174,166,196,127),via(315,166,358,144)),
        floor(225,219,137,0.62f,via(98,181,82,114),via(356,173,429,113)),
        floor(324,224,148,0.67f,via(256,193,374,142),via(398,168,440,136)),
        floor(122,217,135,0.68f,via(160,181,211,145),via(165,176,232,126)),
        floor(246,225,170,0.7f,via(130,219,72,226),via(300,172,334,136)),
        floor(240,223,166,0.68f,via(273,203,285,177),via(356,199,389,173)),
        floor(211,229,149,0.52f,up(145,216,152,141),via(302,214,369,180)),
        floor(248,222,152,0.65f,via(137,195,70,175),via(359,207,438,171)),
        floor(227,196,145,0.56f,via(142,167,117,131),via(284,148,301,113)),
        floor(238,225,137,0.6f,via(144,190,75,145),via(283,202,293,173)),
        floor(277,202,78,0.35f,via(190,197,98,211),via(346,148,399,123)),
        // Paired gates, shutter, supply, final fork (left staircase/right office).
        floor(255,228,125,0.58f,via(130,193,148,104),via(383,193,362,104)),
        floor(252,207,134,0.6f,up(171,183,128,128),via(356,169,398,161)),
        floor(238,209,104,0.47f,via(185,199,153,178),via(277,187,304,167)),
        floor(183,238,103,0.42f,up(98,169,64,92),via(279,231,418,179)),
        // Upper kiosk, speaker, pillar junction, view down to the lower mezzanine.
        floor(249,228,126,0.55f,via(121,217,98,171),via(391,194,433,152)),
        floor(228,228,122,0.54f,via(130,204,173,177),via(339,195,401,188)),
        floor(223,234,122,0.55f,via(139,216,162,168),via(330,228,410,187)),
        floor(363,215,117,0.48f,down(333,200,227,174),down(303,211,184,181)),
        // Maintenance walkway, far platform, switchback landing and service hall.
        floor(422,219,118,0.52f,via(409,193,430,151),via(427,181,447,138)),
        floor(339,223,121,0.6f,up(354,160,347,114),via(298,185,294,148)),
        floor(255,210,110,0.42f,up(177,151,119,68),down(319,211,410,223)),
        floor(201,226,119,0.5f,via(218,185,272,145),via(347,210,430,179))
    };
    static FloorPlan of(int view) { return ROOMS[view]; }
}
