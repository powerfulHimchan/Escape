package com.powerfulhimchan.escape;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class CheckStage {
    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir=new File("app/src/main/res/drawable-nodpi");
        int[] top=new int[6];
        for (String name : new String[]{"walk","away","run","stair"}) {
            BufferedImage sheet=ImageIO.read(new File(dir,"motion_"+name+".png"));
            int size=sheet.getWidth()/6;
            require(size==128 && sheet.getHeight()==3*size,name+" grid");
            for (int row=0;row<3;row++) for (int frame=0;frame<6;frame++) {
                int first=size, last=-1;
                for (int y=0;y<size;y++) for (int x=0;x<size;x++) {
                    if ((sheet.getRGB(frame*size+x,row*size+y)>>>24)>80) {
                        first=Math.min(first,y); last=Math.max(last,y);
                    }
                }
                require(last>=size-5 && first<last,name+" foot registration");
                if (name.equals("stair") && row==2) top[frame]=first;
            }
        }
        FloorPlan floor=FloorPlan.SHUTTER;
        for (float decision : new float[]{0,.5f,.99999f,1}) {
            float previous=0;
            for (int ms=0;ms<=1750;ms++) {
                float p=ms/1750f, y=ShutterPass.y(p,floor.y);
                float drop=ShutterPass.drop(decision,y);
                require(drop>=previous,"shutter reversed");
                previous=drop;
                if (y>=ShutterPass.SILL-16 && y<=ShutterPass.SILL+3) {
                    int frame=ShutterPass.duckFrame(p,y);
                    require(frame>=0,"upright crossing");
                    float head=y-floor.heightAt(y)*(1-top[frame]/128f);
                    float blade=ShutterPass.TOP+(ShutterPass.SILL-ShutterPass.TOP)*drop;
                    require(head>=blade,"shutter meets traveller before trailing foot clears");
                }
            }
            require(previous==1,"shutter did not close after entry");
        }
        FloorPlan gates=FloorPlan.of(25);
        for (FloorPlan.Exit e : new FloorPlan.Exit[]{gates.first,gates.second}) {
            float t=(174-e.turnY)/(float)(e.endY-e.turnY);
            float x=e.turnX+t*(e.endX-e.turnX);
            require((x>114 && x<149 || x>363 && x<399) && e.endY<150,"blocked gate lane");
        }
        System.out.println("Verified 72 registered poses, two traversable gates and shutter clearance from early input through deadline");
    }
}
