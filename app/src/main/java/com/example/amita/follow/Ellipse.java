package com.example.amita.follow;

/**
 * Created by Amita on 2016-08-15.
 */
public class Ellipse extends Path {
    boolean leadBack;
    public String pathDirection; //can either be "straight", "left", or "right"
    String type; //can be either "half series" (ALWAYS not straight) or "normal"
    int maxDuration;
    float radius;
    float startX;
    int screenWidth;
    int xDots=0;



    public Ellipse(String pathDirection, int maxDur, boolean lead, float startingX, int screen_width){
        if (Math.random()>0.5 && leadBack==false){
            type="half series";
        }
        else{
            type="normal";
        }
        leadBack=lead;
        maxDuration=maxDur;

        startX=startingX;

        screenWidth=screen_width;


        if (type.equals("normal")){
            radius=(float)(Math.random()*Math.min(startingX, screenWidth-startX));
        }
        else{
            int a=(int)(Math.random()*5);
            if (pathDirection.equals("left")){
                radius=startingX/a;
            }
            else if (pathDirection.equals("right")){
                radius=(screenWidth-startingX)/a;
            }
        }



    }

    public float getX(){
        return (float)(Math.sqrt(Math.pow(radius,2)-Math.pow(xDots,2)));
    }


}
