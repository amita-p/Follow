package com.example.amita.follow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity  {
    //VARIABLES ASSOCIATED WITH THIS ENTIRE CLASS
    static float spaceBetween; //Space (in both x and y dir) between each dot (in PX!!)
    static float xSpaceBetween;//Space (in y dir) between each dot (in PX!!)
    static float dpSpeed = (float)2; //how fast dots move downward, in DP, can't exceed a certain value, otherwise more than one dot will need to be created each time
    static float pixelSpeed; //how fast dots move downward, in PX
    static float prevX; //prevX and prevY are variables needed to move the black dot with the finger
    static float prevY;
    static int screenHeight;//IN PX
    static int screenWidth;//IN PX
    static int numDots; //number of dots in existance at a given time, always constant
    static double xDots=0; //used to keep track of dots as an x value for the functions, goes back to zero at the start of a new function
    static ArrayList<View> path = new ArrayList<View>(); //arraylist of dots in the path, dots get removed and added from list as they move downward
    static Drawable dotDrawable; //drawable object for a dot
    static Drawable circleDrawable;//circle object for a dot
    static ImageView circleView;//imageview for the circle
    static Handler handler;
    static RelativeLayout layout;//relative layout which everything on the screen is in
    static Context context; //context of the decorview of the window
    static int counter=0; //counts how many times the run method has run sp far
    static int startCounter; //every time we create a new path, we set this equal to 0 to monitor the duration of the path
    static boolean newPathNeedsToBeDecided=true; //set to true when a certain path finishes, initially true
    static String pathType="sine"; //initially, the path will be sine
    public static int maxDuration; //the maximum duration of the current path
    public static float centreInPixels; //x value for the dot drawable such that it is directly in the middle of the screen (x), in PX!!
    public static float lastX; //in pixels!!
    public static float lastY; //in pixels!!
    public static float zeroX; //in pixels!! //the zero value for the path
    public static float padding; //in pixels!!, this determines how close the path can get to the edge of the screen
    public static int dotColor=Color.RED;
    public static double xDotIncrement=1;
    public static boolean touchedRightEdge=false;
    public static boolean touchedLeftEdge=false;

    //parameters for diagonal path
    public static double diagonalPathSlope;

    //parameters for sine path
    public static int sinePeriod; //IN DOTS!
    public static float sineAmplitude; //IN DP!!

    //parameters for circles path
    public static float circlesRadius; //IN DP!!


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //view is the DecorView
        View view = this.getWindow().getDecorView();
        //context of the view
        context = view.getContext();
        //relative layout that everything in this activity is in
        layout = (RelativeLayout)findViewById(R.id.mainLayout);
        //drawable objects, dimensions are specified in the xml
        circleDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.circle, null);
        dotDrawable = ResourcesCompat.getDrawable(getResources(), R.drawable.dot, null);

        //in PIXELS!
        spaceBetween = dotDrawable.getIntrinsicHeight()*2;
        xSpaceBetween = 0;


        pixelSpeed = (convertDpToPixel(dpSpeed, context));

        //width and height of the screen in PIXELS
        screenWidth = getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = getApplicationContext().getResources().getDisplayMetrics().heightPixels;

        lastY=screenHeight;

        //setting the padding
        padding=(float)(0.5*screenWidth*0.1);

        //number of dots in existence at any given time
        numDots = (int)(screenHeight /(spaceBetween + dotDrawable.getIntrinsicHeight()))+2;


        centreInPixels=screenWidth / 2 - (dotDrawable.getIntrinsicWidth()) / 2;
        zeroX=centreInPixels;

        /* setX and setY methods are in PIXELS
         *
         *
         */
        sinePeriod=(numDots/2);
        sineAmplitude=convertPixelsToDp((float)(screenWidth/4), context);


        xDots=0;
        dotColor=Color.RED;
        xSpaceBetween=0;
        //initially create 2000 dot imageview objects and add them to the layout
        for (int i = 0;i<100;i++){
            ImageView dot = new ImageView(context);
            dot.setBackgroundResource(R.drawable.dot);
            GradientDrawable bgShape = (GradientDrawable)dot.getBackground();
            bgShape.setColor(dotColor);

            float a = convertDpToPixel(function(xDots), context);

            dot.setX(zeroX+a);
            lastX=dot.getX();
            if(i!=0) {
                dot.setY((float) (lastY - (xSpaceBetween + (xDotIncrement * dotDrawable.getIntrinsicHeight()))));
            }
            else{
                dot.setY(screenHeight-dotDrawable.getIntrinsicHeight());
            }
            lastY=dot.getY();

            path.add(dot);
            layout.addView(dot);

            xDotIncrement = getXDotIncrement();
            xDots+=xDotIncrement;
            xSpaceBetween=(float)(xDotIncrement*spaceBetween);

        }





        //Creating the circleView
        circleView = new ImageView(context);
        circleView.setImageResource(R.drawable.circle);
        layout.addView(circleView);
        RelativeLayout.LayoutParams circleViewLayoutParams =
                (RelativeLayout.LayoutParams)circleView.getLayoutParams();
        circleViewLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        circleView.setLayoutParams(circleViewLayoutParams);

        //Initially, the ball X value of the ball is the middle of the screen
        prevX = screenWidth/2-circleView.getWidth()/2;






        //creating a new handler for the run method
        /*
         * first path will be pre-created with random path type (for now, it will be the sine wave)
         * one path type will have a duration of 500 to 1500 runs (THESE NUMBERS NEED TO BE CHECKED), randomly choose within this interval
         * the path must always be centred - if one path type leads it off centre, the next must lead it back to the centre or be straight
         * when path type changes, will start off with x position of last dot
         * if path is currently not centred, next path must lead to the centre
         * only circlePath and diagonalPath can lead back to the centre
         * must ensure that path does not run off sides of screen
         *
        */

        handler = new Handler();
        final Runnable r = new Runnable() {
            public void run() {
                counter++;

                if((counter-startCounter)<=maxDuration && ((convertDpToPixel(function(xDots), context)+zeroX) >= padding && pathType.equals("straight")==false) && ((convertDpToPixel(function(xDots), context)+zeroX) <= (screenWidth-padding)&&pathType.equals("straight")==false)){
                    newPathNeedsToBeDecided=false;
                }
                else{
                    newPathNeedsToBeDecided=true;
                }

                //deciding on a new path type if a new path type needs to be decided upon
                if (newPathNeedsToBeDecided){
                    if(dotColor==Color.BLUE){
                        dotColor=Color.RED;
                    }
                    else{
                        dotColor=Color.BLUE;
                    }
                    int a = (int)(Math.round(Math.random()*3));
                    if (a==0){
                       pathType = "straight";
                       maxDuration = (int)((Math.random()*50)+100);
                    }
                    else if (a==1){
                        pathType = "diagonal";
                        diagonalPathSlope=convertPixelsToDp((float)(Math.random()*dotDrawable.getIntrinsicHeight()*4-dotDrawable.getIntrinsicHeight()*2), context);
                        maxDuration = (int)((Math.random()*50)+50);
                    }
                    else if (a==2){
                        pathType = "sine";
                        sinePeriod=(int)(Math.random()*(numDots/2)+numDots/2);
                        sineAmplitude=convertPixelsToDp((float)(Math.random()*screenWidth/2), context);
                        maxDuration = (int)((Math.random()*100)+50);
                    }
                    else if (a==3){
                        pathType="circles";
                        circlesRadius=convertPixelsToDp((float)(Math.random()*(screenWidth/3)),context);
                        maxDuration=(int)(Math.random()*150+50);
                    }

                    newPathNeedsToBeDecided=false;

                    startCounter=counter;
                    xDots=0;
                    zeroX=lastX;
                }



                //create a dot if the last dot in the path has a y value greater than 0
                if (path.get(path.size()-1).getY() > 0) {
                    ImageView dot = new ImageView(context);
                    dot.setBackgroundResource(R.drawable.dot);
                    GradientDrawable bgShape = (GradientDrawable)dot.getBackground();
                    bgShape.setColor(dotColor);

                    float a = convertDpToPixel(function(xDots), context);

                    dot.setX(zeroX+a);
                    lastX=dot.getX();
                    if (xDots==0) {
                        dot.setY((path.get(path.size() - 1).getY()));
                    }
                    else{
                        dot.setY((float) (path.get(path.size() - 1).getY() - (xSpaceBetween + (xDotIncrement * dotDrawable.getIntrinsicHeight()))));
                    }

                    path.add(dot);
                    layout.addView(dot);

                    xDotIncrement = getXDotIncrement();
                    xDots+=xDotIncrement;
                    xSpaceBetween=(float)(xDotIncrement*spaceBetween);

                }

                if(path.get(0).getY()>screenHeight){
                    //remove the first imageview from the path and from the layout
                    layout.removeView(path.get(0));
                    path.remove(0);

                    //remove and add the circleView imageview to always keep it on top
                    layout.removeView(circleView);
                    layout.addView(circleView);
                }

                //shift all the dots downward
                for (int i = 0;i<path.size();i++){
                    path.get(i).setY(path.get(i).getY()+pixelSpeed);

                }




                handler.postDelayed(this, 0);
            }

        };

        handler.postDelayed(r, 0);


        view.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                    float dx;
                    if (event.getAction() == MotionEvent.ACTION_MOVE){
                        dx = event.getX() - prevX;
                    }
                    else{
                        dx = 0;
                    }
                    circleView.setX(circleView.getX() + dx);
                    prevX = event.getX();
                    prevY = event.getY();


                return false;
            }
        });
    }

    public static double getXDotIncrement(){
        double xDotIncrement=0.1;
        float function = convertDpToPixel(function(xDots+xDotIncrement), context)-convertDpToPixel(function(xDots), context);
        double difference = Math.pow(spaceBetween+dotDrawable.getIntrinsicHeight(), 2)-(Math.pow(xDotIncrement*spaceBetween+dotDrawable.getIntrinsicHeight(),2)+Math.pow(function,2));
        double minDifference=difference;
        double minXDotIncrement=xDotIncrement;
        while(xDotIncrement<=1){
            function = convertDpToPixel(function(xDots+xDotIncrement), context)-convertDpToPixel(function(xDots), context);
            difference = Math.pow(spaceBetween+dotDrawable.getIntrinsicHeight(), 2)-(Math.pow(xDotIncrement*spaceBetween+dotDrawable.getIntrinsicHeight(),2)+Math.pow(function,2));
            if(Math.abs(difference)<Math.abs(minDifference)){
                minDifference=difference;
                minXDotIncrement=xDotIncrement;
            }
            xDotIncrement+=0.01;
        }

        return minXDotIncrement;

    }

    //returns x position from centre in DP!!
    public static float[] function(double xDots){
        if (pathType.equals("straight")){
            float[] returnArray={0};
            return returnArray;

        }
        else if (pathType.equals("diagonal")){
            float[] returnArray= {(float)(xDots*diagonalPathSlope)};
            return returnArray;
        }
        else if (pathType.equals("circles")){
            float[] returnArray = {((float)Math.sqrt(Math.pow(circlesRadius,2)-Math.pow(xDots,2))), -1*((float)Math.sqrt(Math.pow(circlesRadius,2)-Math.pow(xDots,2)))};
            return returnArray;
        }
        else {
            float[] returnArray = {(float)(sineAmplitude*Math.sin(xDots*(2*Math.PI/sinePeriod)))};
            return returnArray;
        }



    }






    /**
     * This method converts dp unit to equivalent pixels, depending on device density.
     *
     * @param dp A value in dp (density independent pixels) unit. Which we need to convert into pixels
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent px equivalent to dp depending on device density
     */
    public static float convertDpToPixel(float dp, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

    /**
     * This method converts device specific pixels to density independent pixels.
     *
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @param context Context to get resources and device specific display metrics
     * @return A float value to represent dp equivalent to px value
     */
    public static float convertPixelsToDp(float px, Context context){
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return dp;
    }
}
