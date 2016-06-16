package com.handysparksoft.flyingbirds;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.handysparksoft.driver.R;

public class PreviewActivity extends Activity {

    private Button btnConfig;
    private Button btnPlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //FullScreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Init fonts
        initFonts();

        //Init
        init();

    }

    View.OnTouchListener btnTouchScaleListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.75f).scaleY(0.75f).setDuration(100).start();
                //return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.animate().scaleX(1).scaleY(1).setDuration(100).start();
                //return true;
            }
            return false;
        }
    };

    private void initFonts() {
        //Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/OCRAStd.otf");
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/BuriedBeforeBB_Reg.otf");

        TextView txtAppTitle = (TextView) findViewById(R.id.txtPreviewAppTitle);
        TextView txtAppTitle2 = (TextView) findViewById(R.id.txtPreviewAppTitle2);
        txtAppTitle.setTypeface(typeFace);
        txtAppTitle2.setTypeface(typeFace);
    }

    private void init() {
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnConfig = (Button) findViewById(R.id.btnConfig);

        btnPlay.setOnTouchListener(btnTouchScaleListener);
        btnConfig.setOnTouchListener(btnTouchScaleListener);

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBack();
            }
        });
    }

    private void goBack() {
        Intent returnIntent = new Intent();
        //returnIntent.putExtra("nickname", txtNickNameText);
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_preview, menu);
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
}
