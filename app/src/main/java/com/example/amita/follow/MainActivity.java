package com.example.amita.follow;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
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
    static int numPathTypes=4; //Numbers of different path types
    static int numChange;
    static float spaceBetween; //Space (in y dir) between each dot (in PX!!)
    static double period = 0.1; //period of the sin function, IN DOTS!!
    static float dpSpeed = (float)3.5; //how fast dots move downward, in DP
    static float pixelSpeed; //how fast dots move downward, in PX
    static float prevX; //prevX and prevY are variables needed to move the black dot with the finger
    static float prevY;
    static int screenHeight;//IN PX
    static int screenWidth;//IN PX
    static int numDots; //number of dots in existance at a given time, always constant
    static int xDots=0; //used to keep track of dots as an x value for the functions, goes back to zero at the start of a new function
    static ArrayList<View> path = new ArrayList<View>(); //arraylist of dots in the path, dots get removed and added from list as they move downward
    static Drawable dotDrawable; //drawable object for a dot
    static Drawable circleDrawable;//circle object for a dot
    static ImageView circleView;//imageview for the circle
    static Handler handler;
    static RelativeLayout layout;//relative layout which everything on the screen is in
    static Context context; //context of the decorview of the window
    static int counter=0; //counts how many times the run method has run sp far
    static boolean increaseAmplitude=true; //increaseAmplitude and reduceAmplitude are boolean variables for the sine function
    static boolean reduceAmplitude=false;
    static int duration; //duration of a certain period
    static int startCounter;
    static boolean newPathNeedsToBeDecided=true; //set to true when a certain path finishes, initially true
    static boolean pathOffCentre=false;
    static String lastPathDirection="straight"; //can be either left, right, or straight
    static int numIterToCreateNewDot;
    static String pathType="straight";
    public static Path currentPath;
    public static int maxPossibleDuration=1501; //THIS NUMBER NEEDS TO BE CHECKED!!
    public static int maxDuration;
    public static float centreInPixels;
    public static float lastX; //in pixels!!
    public static float zeroX; //in pixels!!
    public static float padding; //in pixels!!, this determines how close the path can get to the edge of the screen

    //parameters for diagonal path
    public static double diagonalPathSlope;

    //parameters for sine path
    public static int sinePeriod; //IN DOTS!
    public static float sineAmplitude; //IN DP!!


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
        spaceBetween = dotDrawable.getIntrinsicHeight()/2;

        /* pixelSpeed is not a direct conversion of dpSpeed to pixels, but is the closest it can be, such that
         * (dotHeight+spaceBetween)/pixelSpeed is a whole number. Since the dots move down pixelSpeed pixels every time the run
         * method is executed, a new dot needs to be created every (dotHeight+spaceBetween)/pixelSpeed times the run method is run
         * As a result, (dotHeight+spaceBetween)/pixelSpeed has to be a whole number.
         * pixelSpeed cannot be greater than (dotHeight+spaceBetween)*2, otherwise, it'll end up being infinity
         */
        pixelSpeed = (convertDpToPixel(dpSpeed, context));
        if ((Math.round((dotDrawable.getIntrinsicHeight()+ spaceBetween)/pixelSpeed))>=2){
            pixelSpeed = ((dotDrawable.getIntrinsicHeight()+ spaceBetween)/(Math.round((dotDrawable.getIntrinsicHeight()+ spaceBetween)/pixelSpeed)));
        }
        else{
            pixelSpeed = ((dotDrawable.getIntrinsicHeight()+ spaceBetween))/2;
        }

        //width and height of the screen in PIXELS
        screenWidth = getApplicationContext().getResources().getDisplayMetrics().widthPixels;
        screenHeight = getApplicationContext().getResources().getDisplayMetrics().heightPixels;

        //setting the padding
        padding=(float)(0.5*screenWidth*0.8);

        //number of dots in existence at any given time
        numDots = (int)(screenHeight /(spaceBetween + dotDrawable.getIntrinsicHeight()))+2;

        centreInPixels=screenWidth / 2 - (dotDrawable.getIntrinsicWidth()) / 2;

        /* setX and setY methods are in PIXELS
         *
         *
         */
        sinePeriod=(int)(Math.random()*numDots+numDots/2);
        sineAmplitude=convertPixelsToDp((float)(Math.random()*screenWidth/2), context);
        //initially create numdots dot imageview objects and add them to the layout
        for (int i = 0;i<numDots;i++){
            ImageView dot = new ImageView(context);
            dot.setImageResource(R.drawable.dot);
            float a = convertDpToPixel(sineAmplitude*(float)Math.sin((2*Math.PI/sinePeriod)*(xDots)),context);
            dot.setX(centreInPixels + a);
            lastX=dot.getX();
            dot.setY(screenHeight-i*(spaceBetween+dotDrawable.getIntrinsicHeight()));

            path.add(dot);
            layout.addView(dot);

            xDots++;
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

        //how often the run method removes a dot from the bottom and adds one to the top
        numIterToCreateNewDot = (int)((dotDrawable.getIntrinsicHeight()+ spaceBetween)/pixelSpeed);




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
                //System.out.println(counter);
                //pathOffCentre and lastPathOffsetDirection

                 /* paths with leadBack=true have a maxDuration of -1, meaning they can take as long as necessary to lead
                  * back to the centre
                  *
                  *
                  *
                  *
                  */

                if ((counter-startCounter)<maxDuration && (convertDpToPixel(function(), context)*-1+zeroX) > 0 && (convertDpToPixel(function(), context)*-1+zeroX) < screenWidth){
                    newPathNeedsToBeDecided=false;
                }
                else{
                    newPathNeedsToBeDecided=true;
                }

                //deciding on a new path type if a new path type needs to be decided upon
                if (newPathNeedsToBeDecided){
                    int a = (int)(Math.round(Math.random()*2));
                    if (a==0){
                       pathType = "straight";
                    }
                    else if (a==1){
                        pathType = "diagonal";
                        diagonalPathSlope=convertPixelsToDp((float)(Math.random()*dotDrawable.getIntrinsicHeight()*4-dotDrawable.getIntrinsicHeight()*2), context);
                    }
                    else if (a==2){
                        pathType = "sine";
                        sinePeriod=(int)(Math.random()*numDots+numDots/2);
                        sineAmplitude=convertPixelsToDp((float)(Math.random()*screenWidth/2), context);
                    }
                    newPathNeedsToBeDecided=false;
                    maxDuration = (int)((Math.random()*400)+100);
                    System.out.println(pathType);
                    System.out.println("max duration: "+maxDuration);
                    startCounter=counter;
                    xDots=0;
                    zeroX=lastX;
                }






                //if (path == )

                    /*if (convertDpToPixel((float) amplitude, context) < (0.5 * 0.5 * screenWidth) && increaseAmplitude) {
                        amplitude += 0.2;

                    }
                    else if (reduceAmplitude!= true) {
                        increaseAmplitude = false;
                        reduceAmplitude = true;
                        numChange++;
                    }

                    if (convertDpToPixel((float) amplitude, context) > 0 && reduceAmplitude) {
                        amplitude -= 0.2;

                    } else if (increaseAmplitude!=true) {
                        reduceAmplitude = false;
                        increaseAmplitude = true;
                        numChange++;
                        if (numChange==2){
                            numDots=0;
                        }
                    }*/







                //creating a new dot at the top and removing a dot at the bottom
                if (counter%numIterToCreateNewDot==0) {
                    //create a new dot imageview
                    ImageView dot = new ImageView(context);
                    dot.setImageResource(R.drawable.dot);


                    dot.setX(zeroX);


                    float y = path.get(path.size() - 1).getY() - (spaceBetween + dotDrawable.getIntrinsicHeight());
                    dot.setY((y));
                    dot.setX(dot.getX()-convertDpToPixel(function(), context));
                    lastX=dot.getX();

                    //add the imageview to the path arraylist
                    path.add(dot);

                    //add the imageview to the layout
                    layout.addView(dot);

                    //remove the first imageview from the path and from the layout
                    layout.removeView(path.get(0));
                    path.remove(0);

                    //remove and add the circleView imageview to always keep it on top
                    layout.removeView(circleView);
                    layout.addView(circleView);

                    //increase the xDots by 1
                    xDots++;
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

    //returns x position from centre in DP!!
    public static float function(){
        if (pathType.equals("straight")){
            return 0;
        }
        else if (pathType.equals("diagonal")){
            return (float)(xDots*diagonalPathSlope);
        }
        else {
            return (float)(sineAmplitude*Math.sin(xDots*(2*Math.PI/sinePeriod)));
        }
    }

    //will decide the direction of the next path, taking the direction of the last path into account
    public static String decidePathDirection(){
        if (lastPathDirection.equals("straight")==false){
            if (Math.random()>0.5){
                return "straight";
            }
            else{
                if (lastPathDirection.equals("left")){
                    return "right";
                }
                else{
                    return "left";
                }
            }
        }
        else{
            if (Math.random()>0.5){
                return "right";
            }
            else{
                return "left";
            }
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
