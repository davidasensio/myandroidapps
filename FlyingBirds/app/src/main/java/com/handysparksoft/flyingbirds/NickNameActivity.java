package com.handysparksoft.flyingbirds;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.handysparksoft.driver.R;

public class NickNameActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nick_name);



        final TextView txtNickName = (TextView) findViewById(R.id.txtNickName);
        Button btnOkNickName = (Button) findViewById(R.id.btnOkNickname);
        btnOkNickName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String txtNickNameText = txtNickName.getText().toString();
                if (txtNickNameText != null && txtNickNameText.length() > 0) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("nickname", txtNickNameText);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                }
            }
        });

        //Init dimension
        //initDimension();

        //Init fonts
        initFonts();


    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //initDimension();
    }

    private void initDimension() {
        WindowManager.LayoutParams params = getWindow().getAttributes();

        params.width = params.width + 100;
        this.getWindow().setAttributes(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_nick_name, menu);
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


    private void initFonts() {
        //Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/OCRAStd.otf");
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/BuriedBeforeBB_Reg.otf");

        TextView labelNickname = (TextView) findViewById(R.id.labelNickName);
        labelNickname.setTypeface(typeFace);

        EditText txtNickname = (EditText) findViewById(R.id.txtNickName);
        txtNickname.setTypeface(typeFace);

        Button btnOkNickname = (Button) findViewById(R.id.btnOkNickname);
        btnOkNickname.setTypeface(typeFace);
    }
}
