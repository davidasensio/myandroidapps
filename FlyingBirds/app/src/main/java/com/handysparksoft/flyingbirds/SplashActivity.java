package com.handysparksoft.flyingbirds;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;

import com.handysparksoft.driver.R;

import java.util.Timer;
import java.util.TimerTask;



public class SplashActivity extends Activity {

    private static long SPLASH_SCREEN_DELAY = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //FullScreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        SPLASH_SCREEN_DELAY = getResources().getInteger(R.integer.splash_screen_duration);
        setContentView(R.layout.activity_splash);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {

                // Start the next activity
                Intent mainIntent = new Intent().setClass(SplashActivity.this, GameActivity.class);
                startActivity(mainIntent);

                // Close the activity so the user won't able to go back this
                // activity pressing Back button
                finish();
            }
        };

        // Simulate a long loading process on application startup.
        Timer timer = new Timer();
        timer.schedule(task, SPLASH_SCREEN_DELAY);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_splash, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        final ImageView txtLoading = (ImageView) findViewById(R.id.imgLoading);
        //animateScaleView(1, txtLoading);
        animateAlphaView(1, txtLoading);
    }

    private void animateScaleView(final int counter, final View view) {
        final int repeatCount = 2;
        float scaleFactor = 1.7f;


        Animation animation1 = new ScaleAnimation(1, scaleFactor, 1, scaleFactor, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation1.setDuration(500);
        final Animation animation2 = new ScaleAnimation(scaleFactor, 1, scaleFactor, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation2.setDuration(500);

        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setAnimation(animation2);
                //animation2.start();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        animation2.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (counter < repeatCount) {
                    animateScaleView(counter + 1, view);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        view.setAnimation(animation1);
        view.getAnimation().start();
    }

    private void animateAlphaView(final int counter, final View view) {
        final int repeatCount = 3;
        float scaleFactor = 1.7f;



        Animation animation1 = new AlphaAnimation(0.2f,1);
        animation1.setDuration(1000);


        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (counter < repeatCount) {
                    animateAlphaView(counter + 1, view);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        view.setAnimation(animation1);
        view.getAnimation().start();
    }
}
