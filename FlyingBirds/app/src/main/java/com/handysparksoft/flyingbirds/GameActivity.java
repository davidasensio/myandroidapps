package com.handysparksoft.flyingbirds;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.handysparksoft.driver.NickNameActivity;
import com.handysparksoft.driver.PreviewActivity;
import com.handysparksoft.driver.R;
import com.handysparksoft.driver.UserAccountInfo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;


public class GameActivity extends Activity implements SensorEventListener {

    //Constants
    private static final String LOG_TAG = GameActivity.class.getSimpleName();
    public static final int FPS_GAME = 15;
    public static final int BIRD_TYPES_COUNT = 20; //Up to 30 birds simultaneosly
    public static final int BIRD_HEIGHT = 150;
    public static final float BIRD_PLAYER_MOVE_SPEED = 5f;
    public static final float BIRD_PLAYER_MOVE_THERESOLD = 0.2f;
    public static final float BIRD_PLAYER_EFFECTIVE_WIDTH_DIVIDE_FACTOR = 1.6f;
    public static final float BIRD_RADIOUS_CRASH = 75;
    public static final int GAME_LIVES = 3;
    private static final float JUMP_DISTANCE = 130;

    //Level Constants
    public static final int BIRDS_CREATION_LEVEL = 5; //[1..10]
    public static final long BIRDS_SPEED_BASE_ANIMATION = 8500; //6500
    private static final long GRAVITY_MILLIS = 1200;

    //Level Default values
    int birdsCreationLevel = BIRDS_CREATION_LEVEL;
    long birdsSpeedAnimation = BIRDS_SPEED_BASE_ANIMATION;

    //Components
    Button player = null;
    ImageView bg = null;
    RelativeLayout mainLayout = null;
    TextView txtNextLevel = null;
    Button btnGameMenu = null;

    Timer timer;
    TimerTask timerTask;

    //Sensors
    SensorManager sensorManager = null;
    Sensor lightSensor = null;
    Sensor accelerometerSensor = null;

    boolean flappyBirdMode = false;

    //Background
    float w = 0;
    float h = 0;
    float rearPosition = 0;
    int backgroundSceneDuration = 25000;

    private float scaleFactor;
    private float adjustFactor = 1.23f;
    private final Matrix matrixBackground = new Matrix();
    private ValueAnimator animatorBackground;
    private android.graphics.RectF displayRect = new RectF();

    //Logic
    boolean initialized = false;
    boolean countdownDone = false;
    boolean firstTime = true;
    private Float playerInitialX;
    private Float playerInitialY;

    float lux = 0;
    float maxLux = 0;
    int birdCounter = 0;
    int speedX = 30;

    ArrayList<Bird> birds = new ArrayList<>();
    private Drawable randomBird;

    //Game
    private int level = 0;
    private long score = 0;
    private int lives = GAME_LIVES;
    private long showEndGameDuration = 5000;
    private int countDownStart;
    private long startGameTime;

    private Handler mUiHandler = new Handler();

    //Activities
    public static final int NICKNAME_ACTIVITY_REQUEST_CODE = 10;
    public static final int PREVIEW_ACTIVITY_REQUEST_CODE = 20;

    //User data
    private UserAccountInfo userAccountInfo;
    private FireBaseManager fireBaseManager;
    private boolean isGamePaused = false;


    /* Application life cycle */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //FullScreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_driver);
        //setContentView(new BackgroundMove(this));

        //Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //Set components
        player = (Button) findViewById(R.id.btn);
        txtNextLevel = (TextView) findViewById(R.id.txtNextLevel);
        btnGameMenu = (Button) findViewById(R.id.btnGameMenu);
        btnGameMenu.setOnTouchListener(btnTouchScaleListener);
        btnGameMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endGame(false);
                showGamePreview();
            }
        });

        //Init sensors
        flappyBirdMode = true; //FIXME: parametrize
        if (!flappyBirdMode) {
            initSensors();
        }

        //Init fonts
        initFonts();

        //User data
        userAccountInfo = new UserAccountInfo(this);
        fireBaseManager = new FireBaseManager(this);
        fireBaseManager.initFireBase();

        //firstTime flag
        firstTime = true;

        //Establish Parameters
        establishParameters();
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

    private void showGamePreview() {
        Intent intentPreview = new Intent(this, PreviewActivity.class);
        startActivityForResult(intentPreview, PREVIEW_ACTIVITY_REQUEST_CODE);
    }

    private void establishParameters() {
        showEndGameDuration = getResources().getInteger(R.integer.game_over_duration);
        countDownStart = getResources().getInteger(R.integer.game_count_down_start);
    }


    private void updatePreferences() {
        //Update accessCounter
        long actualCounter = getPrefs().getLong("counterAccess", 0);
        actualCounter++;
        saveLongPreference("counterAccess", actualCounter);

        //Update accessDurationAverage
        long actualGameDuration = getGameDuration();
        long accessTotalDuration = getPrefs().getLong("accessTotalDuration", 0);
        accessTotalDuration += actualGameDuration;
        long accessDurationAverage = accessTotalDuration / actualCounter;

        saveLongPreference("accessTotalDuration", accessTotalDuration);
        saveLongPreference("accessDurationAverage", accessDurationAverage);
    }

    private void updateFirebaseData() {
        String keyUsername = "username";
        String keyUsermail = "usermail";
        String keyNickname = "nickname";
        String keyLastAccess = "lastAccess";
        String keyAccessTotalDuration = "accessTotalDuration";
        String keyAccessDurationAverage = "accessDurationAverage";
        String keyCounterAccess = "counterAccess";
        String keyDeviceModel = "deviceModel";
        String keyMaxScore = "maxScore";
        String keyMaxLevel = "maxLevel";

        String keyHistoricAccess = "historicAccess";

        Map<String, String> mapData = new HashMap<>();

        //Fill data
        long accessTotalDuration = getPrefs().getLong(keyAccessTotalDuration, 0);
        long accessDurationAvg = getPrefs().getLong(keyAccessDurationAverage, 0);
        long maxScore = getPrefs().getLong(keyMaxScore, 0);
        long maxLevel = getPrefs().getLong(keyMaxLevel, 0);

        mapData.put(keyUsername, userAccountInfo.getFormattedUserAccount());
        mapData.put(keyUsermail, userAccountInfo.getUserAccount());
        mapData.put(keyNickname, getPrefs().getString(keyNickname, ""));
        mapData.put(keyLastAccess, Calendar.getInstance().getTime().toString());
        mapData.put(keyAccessTotalDuration, String.valueOf(accessTotalDuration + " seconds -> " + Long.valueOf(accessTotalDuration / 60)) + " min(s)");
        mapData.put(keyAccessDurationAverage, String.valueOf(accessDurationAvg + " seconds -> " + Long.valueOf(accessDurationAvg / 60)) + " min(s)");
        mapData.put(keyCounterAccess, String.valueOf(getPrefs().getLong(keyCounterAccess, 0)));
        mapData.put(keyDeviceModel, getDeviceModel());
        mapData.put(keyMaxScore, String.valueOf(maxScore));
        mapData.put(keyMaxLevel, String.valueOf(maxLevel));

        //mapData.put(keyHistoricAccess, "david@gmail.com");

        String userId = getUserId();
        fireBaseManager.storeUserDataInFireBase(userId, mapData);
    }

    private String getDeviceModel() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        String result = manufacturer + " " + model;
        return result;
    }

    private void initFonts() {
        //Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/OCRAStd.otf");
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "fonts/BuriedBeforeBB_Reg.otf");

        TextView textViewScore = (TextView) findViewById(R.id.txtScore);
        TextView textViewLives = (TextView) findViewById(R.id.txtLives);
        TextView txtGameOver = (TextView) findViewById(R.id.txtGameOver);
        TextView txtGameOver2 = (TextView) findViewById(R.id.txtGameOver2);

        textViewScore.setTypeface(typeFace);
        textViewLives.setTypeface(typeFace);
        txtGameOver.setTypeface(typeFace);
        txtGameOver2.setTypeface(typeFace);
        txtNextLevel.setTypeface(typeFace);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_GAME);
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        startGameTime = System.currentTimeMillis();

        if (isGamePaused) {
            unpauseGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (sensorManager != null) {
            sensorManager.unregisterListener(this, lightSensor);
            sensorManager.unregisterListener(this, accelerometerSensor);
        }
        pauseGame();

        //Update preferences and Firebase data
        updatePreferences();
        updateFirebaseData();
    }

    private long getGameDuration() {
        //Game duration in secs
        return ((System.currentTimeMillis() - startGameTime) / 1000);
    }

    private void unpauseGame() {
        if (initialized) {
            continueGame();
        }
    }

    private void pauseGame() {
        isGamePaused = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (mainLayout != null) {
            mainLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                }
            });
        }


    }

    private void endGame(Boolean autoPlay) {
        initialized = false;
        pauseGame();
        stopAnimation(animatorBackground);
        clearBirds();
        showGameOver(true);

        //Save score
        saveUserScoreAndLevel();

        if (autoPlay) {
            new Handler().postDelayed(new Runnable() {
                                          @Override
                                          public void run() {
                                              initGame();
                                          }
                                      }, showEndGameDuration
            );
        }
    }

    private void saveUserScoreAndLevel() {
        long actualScore = getScore();
        long actualLevel = getLevel();
        long maxScore = getPrefs().getLong("maxScore", 0);
        long maxLevel = getPrefs().getLong("maxLevel", 0);

        if (actualScore > maxScore) {
            saveLongPreference("maxScore", actualScore);
        }
        if (actualLevel > maxLevel) {
            saveLongPreference("maxLevel", actualLevel);
        }
    }

    private void showGameOver(boolean value) {
        TextView txtGameOver = (TextView) findViewById(R.id.txtGameOver);
        TextView txtGameOver2 = (TextView) findViewById(R.id.txtGameOver2);
        if (value) {
            txtGameOver.setVisibility(View.VISIBLE);
            txtGameOver2.setVisibility(View.VISIBLE);
        } else {
            txtGameOver.setVisibility(View.INVISIBLE);
            txtGameOver2.setVisibility(View.INVISIBLE);
        }

    }

    private void clearBirds() {
        for (final Bird bird : birds) {
            bird.setAnimation(new AlphaAnimation(1, 0));
            bird.getAnimation().setDuration(500);
            bird.getAnimation().start();
            bird.getAnimation().setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    removeBird(bird);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_driver, menu);
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
    public void onSensorChanged(SensorEvent event) {
        updateBirdPlayerBySensorEvent(event);
    }

    private void updateBirdPlayerBySensorEvent(SensorEvent event) {
        if (event.sensor.equals(lightSensor)) {
            lux = event.values[0];
            //updatePositionByLux(lux);
        } else if (event.sensor.equals(accelerometerSensor)) {
            float x = event.values[1];
            float y = event.values[0];
            float z = event.values[2];
            updatePositionByAccelerometer(x, y, z);
        }
    }

    private void updateBirdPlayerByTouchEventV1() {

        TimerTask timerGravity = new TimerTask() {
            @Override
            public void run() {
                float effectiveH = h - BIRD_HEIGHT;
                float newY = player.getY();

                newY = newY + 15;
                if (newY > 0 && newY < effectiveH) {
                    player.setY(newY);
                }
            }
        };

        new Timer().schedule(timerGravity, 0, 25);

        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                updatePositionByTouch();
                return true;
            }
        });
    }


    private void updateBirdPlayerByTouchEvent() {
        animateGravityPlayer();

        /*
        final Handler handler = new Handler();
        final Runnable mLongPressed = new Runnable() {
            public void run() {
                animateJumpPlayer();
            }
        };
        */

        mainLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {


                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        //handler.postDelayed(mLongPressed, 500);
                        animateJumpPlayer();
                        break;
                    case MotionEvent.ACTION_UP:
                        //handler.removeCallbacks(mLongPressed);
                        break;
                    default:
                        break;

                }

                //animateJumpPlayer();
                return true;
            }
        });
    }

    private void animateGravityPlayer() {
        final float effectiveH = h - BIRD_HEIGHT;
        final float limitY = effectiveH + (BIRD_HEIGHT / 3);
        float distance = (effectiveH - player.getY()) * 1;
        float dY = effectiveH * 2f;

        ValueAnimator.AnimatorUpdateListener listener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (player.getY() > limitY) {
                    animation.cancel();
                    endGame(true);
                }

                if (isGamePaused) {
                    animation.cancel();
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            player.animate().translationY(dY).setDuration(GRAVITY_MILLIS).setUpdateListener(listener).start();
        } else {
            ObjectAnimator oa = ObjectAnimator.ofFloat(player, View.TRANSLATION_Y, dY);
            oa.setDuration(GRAVITY_MILLIS);
            oa.start();
        }


    }

    private void animateJumpPlayer() {
        float effectiveH = h - BIRD_HEIGHT;
        float newY = player.getY();
        effectiveH = effectiveH * 1.5f;

        float jumpDistance = JUMP_DISTANCE;
        if (jumpDistance > newY) {
            jumpDistance = newY + 25;  //Ceil corrector factor
        }

        newY = newY - jumpDistance; // - player.getAnimation().getStartTime();
        //if (newY > -100) { //&& newY < effectiveH) {
        //player.setY(newY);
        player.animate().translationY(newY).setDuration(50).withEndAction(new Runnable() {
            @Override
            public void run() {
                animateGravityPlayer();
            }
        }).start();
        //}
        //else {
        //    animateGravityPlayer();
        //}

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        initGame();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (NICKNAME_ACTIVITY_REQUEST_CODE == requestCode) {
            if (resultCode == RESULT_OK) {
                String nickname = data.getStringExtra("nickname");

                //Save to sharedpreferences
                saveStringPreference("nickname", nickname);

                //Update firebase
                fireBaseManager.storeUserNickname(getUserId(), nickname);

                //Init game Continues
                setDynamicBackground();
            }
        }

        if (PREVIEW_ACTIVITY_REQUEST_CODE == requestCode) {
            prepareGame();
        }
    }

    private void saveStringPreference(String key, String value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void saveLongPreference(String key, Long value) {
        SharedPreferences.Editor editor = getPrefs().edit();
        editor.putLong(key, value);
        editor.apply();
    }

    private boolean checkNickName() {
        boolean result = false;

        String nickname = getPrefs().getString("nickname", "");
        if (nickname != null && nickname.length() > 0) {
            result = true;
        }
        return result;
    }

    public void syncNickname() {
        fireBaseManager.syncUserNickname(getUserId());
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void initGame() {
        if (!initialized) {
            initialized = true;
            setScore(0);
            lives = GAME_LIVES;
            setLevel(0);
            updateScore();
            updateLives();
            showGameOver(false);

            //Default values
            birdsCreationLevel = BIRDS_CREATION_LEVEL;
            birdsSpeedAnimation = BIRDS_SPEED_BASE_ANIMATION;

            //Init layout variable
            if (mainLayout == null) {
                mainLayout = (RelativeLayout) findViewById(R.id.mainLayout);
            }

            //Set the image background without animation
            setStaticBackground();



            //Show Game Preview
            if (firstTime) {
                showGamePreview();
            } else {

                prepareGame();
            }


        }
    }

    public void prepareGame() {
        //Memorize Player initial position
        setInitialPosition();

        //Check NickName
        boolean nickNameEstablished = checkNickName();
        if (!nickNameEstablished) {
            Intent intent = new Intent(this, NickNameActivity.class);
            startActivityForResult(intent, NICKNAME_ACTIVITY_REQUEST_CODE);
        } else {
            setDynamicBackground();
        }
    }

    private void setInitialPosition() {
        if (playerInitialX == null || playerInitialY == null) {
            playerInitialX = player.getX();
            playerInitialY = player.getY();
        }

        player.setX(playerInitialX);
        player.setY(playerInitialY);
    }

    private void setDynamicBackground() {
        //Animate background
        animateBackground();

        //Animate player
        animatePlayer();

        startAll();
        //initialized = true;
    }

    private void setStaticBackground() {
        //Moving background
        setBackground();

        updateBackgroundDimensions();
    }

    private void startAll() {


        if (firstTime) {
            firstTime = false;
            startCountDown(countDownStart);
        } else {
            startGame();
        }
        //startTimer();y

    }


    private void startCountDown(final int counter) {
        ImageView ivCountDown = (ImageView) findViewById(R.id.imageViewCountDown);
        ivCountDown.setVisibility(View.VISIBLE);
        int resId = -99;
        if (counter >= -1) {
            switch (counter) {
                case 3:
                    resId = R.drawable.three;
                    break;
                case 2:
                    resId = R.drawable.two;
                    break;
                case 1:
                    resId = R.drawable.one;
                    break;
                case 0:
                    resId = R.drawable.zero;
                    break;
                case -1:
                    resId = R.drawable.go;
                    break;
                default:


            }

            if (resId != -99) {
                ivCountDown.setImageResource(resId);

                int duration = 1000;
                float initScale = 1f;
                float endScale = 0.1f;

                if (counter == -1) {
                    duration = 500;
                    initScale = 2f;
                    endScale = 0f;
                }

                ivCountDown.setScaleX(initScale);
                ivCountDown.setScaleY(initScale);

                ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(ivCountDown, "scaleX", endScale);
                ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(ivCountDown, "scaleY", endScale);


                scaleDownX.setDuration(duration);
                scaleDownY.setDuration(duration);

                AnimatorSet scaleCountDown = new AnimatorSet();

                scaleCountDown.play(scaleDownX).with(scaleDownY);
                scaleCountDown.start();
                scaleCountDown.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        startCountDown(counter - 1);

                        if (counter == -1) {
                            countdownDone = true;
                        }
                    }
                });
            }
        } else {
            startGame();
        }

    }

    private void startGame() {


        continueGame();
    }

    private void continueGame() {
        isGamePaused = false;
        //Configure Game flow
        configureTimer();

        //Start touch listener
        if (flappyBirdMode) {
            updateBirdPlayerByTouchEvent();
        }

        //Start
        startTimer();


    }

    private void startTimer() {
        if (timer == null) {
            timer = new Timer();
            timer.schedule(timerTask, 1000, 1000 / FPS_GAME);
        }
    }


    /* Initialization */
    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        //Log.d(this.getLocalClassName(), "Max Lux: " + String.valueOf(lightSensor.getMinDelay()));
    }

    private boolean allowNewBird = true;

    private void configureTimer() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (countdownDone) {
                    if ((System.currentTimeMillis() / 12) % birdsCreationLevel == 0) { //Minimal time separation 500 ms [% 5 = 500 ms, 3 = 300 ms, 2 = 200 ms, 1 = 100ms]
                        allowNewBird = !allowNewBird;

                        if (allowNewBird && birdCounter < BIRD_TYPES_COUNT && Math.random() > 0.3) {
                            createBird();
                            birdCounter++;
                        }
                    }
                }
            }
        };
    }

    private void addScore() {
        if (initialized) {
            setScore(getScore() + 5);

            if (getScore() % 50 == 0) {
                nextLevel();
            }
        }


        updateScore();
    }

    private void nextLevel() {
        setLevel(getLevel() + 1);

        txtNextLevel.setText("Level " + level + " achieved!");
        txtNextLevel.setVisibility(View.VISIBLE);
        txtNextLevel.animate().scaleX(2).scaleY(2).setDuration(1000).withEndAction(new Runnable() {
            @Override
            public void run() {
                txtNextLevel.setScaleX(1);
                txtNextLevel.setScaleY(1);
                txtNextLevel.setVisibility(View.INVISIBLE);
            }
        }).start();

        //Increase difficult
        if (level % 100 == 0 && birdsCreationLevel > 1) {
            birdsCreationLevel--;
        }
        if (birdsSpeedAnimation > 2000) {
            birdsSpeedAnimation = birdsSpeedAnimation - 200;
        }

    }

    private void updateScore() {
        final TextView txtScore = (TextView) findViewById(R.id.txtScore);
        txtScore.post(new Runnable() {
            @Override
            public void run() {
                txtScore.setText("Level: " + getLevel());
            }
        });
    }

    private void updateLives() {
        final TextView txtLives = (TextView) findViewById(R.id.txtLives);
        txtLives.post(new Runnable() {
            @Override
            public void run() {
                txtLives.setText("LIVES: " + lives);
            }
        });
    }

    private void setBackground() {


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bg = (ImageView) findViewById(R.id.imageView);
                //bg.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                double randomNumber = Math.random();
                if (randomNumber < 0.5) {
                    bg.setImageResource(R.drawable.heaven_day_c);
                } else {
                    bg.setImageResource(R.drawable.heaven_night);
                }

                matrixBackground.reset();
                bg.setScaleType(ImageView.ScaleType.MATRIX);
                scaleFactor = Float.valueOf(bg.getHeight()) / Float.valueOf(bg.getDrawable().getIntrinsicHeight());
                if (scaleFactor < 1) {
                    scaleFactor = 1;
                }
                matrixBackground.postScale(scaleFactor, scaleFactor);
                bg.setImageMatrix(matrixBackground);

                Log.d(LOG_TAG, "ImgView width (scaleFactor): " + scaleFactor);
                Log.d(LOG_TAG, "ImgView width, height: " + bg.getWidth() + ", " + bg.getHeight());
                Log.d(LOG_TAG, "ImgView width intrins, height intrins: " + bg.getDrawable().getIntrinsicWidth() + ", " + bg.getDrawable().getIntrinsicHeight());
            }
        });
    }

    private void animateBackground() {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
        updateDisplayRect();
        animate(displayRect.left, displayRect.left + ((displayRect.right / 2)));
        //}
        //});

    }

    private void animate(final float from, final float to) {
        animatorBackground = ValueAnimator.ofFloat(from, to);
        animatorBackground.setInterpolator(null);
        animatorBackground.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                matrixBackground.reset();
                matrixBackground.postScale(scaleFactor, scaleFactor);
                matrixBackground.postTranslate(-1 * value, 0);
                bg.setImageMatrix(matrixBackground);

            }
        });
        animatorBackground.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                //animate(from, to);

            }
        });
        animatorBackground.setDuration(backgroundSceneDuration);
        animatorBackground.setRepeatCount(Animation.INFINITE);
        animatorBackground.start();

    }

    private void updateDisplayRect() {
        //bg.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        displayRect.set(0, 0, bg.getDrawable().getIntrinsicWidth(), bg.getDrawable().getIntrinsicHeight());
        matrixBackground.mapRect(displayRect);
    }


    private void updateBackgroundDimensions() {

        if (mainLayout != null) {
            w = mainLayout.getWidth();
            h = mainLayout.getHeight();
            rearPosition = w * 1.4f;
            Log.d(LOG_TAG, "Dimensions: " + w + " X " + h);
        }
    }

    private void removeBird(final View bird) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mainLayout != null) {
                    mainLayout.removeView(bird);
                    birdCounter--;
                }
            }
        });

    }

    private void createBird() {
        final Context context = this;
        /*runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createBirdInContext(context);
            }
        });*/

        /*
        MyWorkerThread myWorkerThread = new MyWorkerThread("bird");
        myWorkerThread.prepareHandler();
        myWorkerThread.postTask(new Runnable() {
            @Override
            public void run() {
                mUiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        createBirdInContext(context);
                    }
                });

            }
        });*/


        mUiHandler.post(new Runnable() {
            @Override
            public void run() {
                createBirdInContext(context);
            }
        });

    }

    private void createBirdInContext(Context context) {
        final float effectiveH = h - BIRD_HEIGHT;
        final Bird bird = new Bird(context);
        bird.setTag("bird " + birdCounter);


        final Integer trayectory = Float.valueOf(getR(8)).intValue();
        final int trayectoryStart = Float.valueOf(getR(400)).intValue();

        //Posicion inicial X
        bird.setInitialPositionX(1500);
        bird.setX(bird.getInitialPositionX()); //w * 1.3f);

        //Posicion inicial Y
        bird.setInitialPositionY((float) ((effectiveH / 5) + (Math.random() * (effectiveH / 1.5))));
        bird.setY(bird.getInitialPositionY());

        //Animate
        int direccion = 1;
        if (bird.getY() > h * 0.4) {
            direccion = -1;
        }

        //Random values
        final float curveHeightStart = getR(50f);
        final float curvePronunciation = getR(3f);

        ValueAnimator vAnimator = ValueAnimator.ofFloat(rearPosition, rearPosition / -2); //FIXME: change 2 with -2
        vAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (Float) animation.getAnimatedValue();
                bird.setX(value);

                if (bird.getX() > player.getX() - BIRD_RADIOUS_CRASH && bird.getX() < player.getX() + (BIRD_RADIOUS_CRASH / 2)) {
                    if (bird.getY() > player.getY() - BIRD_RADIOUS_CRASH && bird.getY() < player.getY() + BIRD_RADIOUS_CRASH) {

                        //Crash of birds
                        crashOfBirds(bird);

                        animation.cancel();
                    }
                }

                //Parabolic
                float x = value;
                float y = 0;


                switch (trayectory) {
                    case 1:
                        float factorParabola = 10 + Math.abs(effectiveH - bird.getInitialPositionY()) * 3;
                        y = bird.getY() - (x / factorParabola);
                        break;
                    case 2:
                        y = bird.getY() - 1 - Double.valueOf(Math.log(x) / (Math.random() * 10 + (bird.getInitialPositionY() / 250))).floatValue();//bird.getY() +  Double.valueOf(Math.sin(bird.getY())).floatValue();
                        break;

                    case 3: //Parabolic
                        //x = x - getR(400);
                        //y = curveHeightStart + ((curvePronunciation * x * x - 4 * x - 2) / 2000);
                        y = curveHeightStart + ((curvePronunciation * x * x - 700 * x - 300) / 2000);
                        //         h     pronunciacion_curva
                        break;
                    case 4: //Parabolic 2
                        y = curveHeightStart * 2 + ((curvePronunciation * x * x - 1000 * x - 1000) / 2500);

                        break;
                    case 5: //Parabolic 3
                        y = curveHeightStart + 10 + ((curvePronunciation * x * x - 2300 * x - 200) / 1500);

                        break;
                    case 6: //Sinoidal
                        y = bird.getInitialPositionY() + (bird.getInitialPositionY() * (Double.valueOf(Math.cos(x / 100) / 5).floatValue())); //Math.cos(x / reduccion amplitud) / reduccion altura
                        break;
                    case 7: //Atan
                        y = bird.getInitialPositionY() + (bird.getInitialPositionY() * (Double.valueOf(Math.atan((x - 500) / 100) / 4).floatValue())); //Math.cos(x / reduccion amplitud) / reduccion altura
                        break;
                    case 8:
                        y = bird.getInitialPositionY() + (bird.getInitialPositionY() * (Double.valueOf(Math.atan(x / 300) / 2).floatValue())); //Math.cos(x / reduccion amplitud) / reduccion altura
                        break;
                    default:
                        y = 400 * (Double.valueOf(Math.atan(x)).floatValue());
                        break;
                }


                //x = x - trayectoryStart;
                bird.setY(y);
            }
        });
        vAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);

                if (bird.getHasCrashed()) {
                    animateFallingBird(effectiveH, bird);
                } else {
                    removeBird(bird);
                    addScore();
                }
            }
        });


        long durationBird = birdsSpeedAnimation - getR(2000).longValue();

        vAnimator.setDuration(durationBird);
        vAnimator.start();


        int bw = getResources().getDimensionPixelSize(R.dimen.bird_width);
        int bh = getResources().getDimensionPixelSize(R.dimen.bird_height);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(bw, bh);
        bird.setLayoutParams(layoutParams);

        bird.setBackground(getRandomBird());


        if (mainLayout != null) {
            mainLayout.addView(bird);
        }


        birds.add(bird);
    }

    private void crashOfBirds(Bird bird) {
        bird.setHasCrashed(true);

        //Touch Alpha animation
        animateCrashAnimation();

        //Remove one life
        if (initialized) {
            lives--;
            if (lives == 0) {
                initialized = false;
                //Game Over
                endGame(true);
            } else {
                if (lives < 0) {
                    lives = 0;
                }
            }
        }

        updateLives();

    }

    private void animateFallingBird(float effectiveH, final Bird bird) {
        long fallingAnimationDuration = 600;

        //Falling animation
        final Animation animationFall = new TranslateAnimation(30, -100, 0, effectiveH * 2);
        animationFall.setDuration(fallingAnimationDuration);
        animationFall.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {

                removeBird(bird);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        bird.setAnimation(animationFall);
        //bird.getAnimation().start();
    }

    private void animateCrashAnimation() {
        AnimationSet animationSet = new AnimationSet(true);
        Animation animation1 = new AlphaAnimation(0.2f, 0.8f);
        animation1.setDuration(550);
        //animation1.setRepeatCount(2);

        //Animation animation2  = new RotateAnimation(0,55,Animation.RELATIVE_TO_PARENT,0.5f,Animation.RELATIVE_TO_PARENT,0.5f);
        //animation2.setDuration(500);
        //animationSet.addAnimation(animation1);
        //animationSet.addAnimation(animation2);
        player.startAnimation(animation1);
        player.getAnimation().setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                animatePlayer();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    private Float getR(float i) {
        return Double.valueOf(Math.random() * i).floatValue() + 1;
    }

    private void animatePlayer() {
        //Fly animation
        if (!flappyBirdMode) {
            player.setAnimation(new TranslateAnimation(10, 0, 0, 20));
            player.getAnimation().setRepeatCount(Animation.INFINITE);
            player.getAnimation().setDuration(850);
            player.getAnimation().start();
        }
    }

    private void closeOpenEyes() {

        player.post(new Runnable() {
            @Override
            public void run() {
                player.setPressed(true);
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        player.post(new Runnable() {
                            @Override
                            public void run() {
                                player.setPressed(false);
                            }
                        });
                    }
                }, 300);
            }
        });


    }

    private void updatePositionByTouch() {
        float effectiveH = h - BIRD_HEIGHT;
        float newY = player.getY();

        newY = 20;//newY - 90;
        if (newY > 0 && newY < effectiveH) {
            player.setY(newY);
        }

    }

    private void updatePositionByLux(float lux) {
        if (player != null && w > 0 && h > 0) {
            float effectiveH = h - BIRD_HEIGHT;

            if (lux > maxLux) {
                maxLux = lux;
                Log.d(this.getLocalClassName(), "Max Lux: " + String.valueOf(maxLux));
            }

            if (maxLux > 0) {
                float newY = (lux * effectiveH / maxLux);
                if (newY > effectiveH) {
                    newY = effectiveH;
                }
                player.setY(effectiveH - newY);
            }
        }
    }

    private void updatePositionByAccelerometer(float x, float y, float z) {
        if (countdownDone && player != null && w > 0 && h > 0) {
            float effectiveH = h - BIRD_HEIGHT;
            float effectiveW = w / BIRD_PLAYER_EFFECTIVE_WIDTH_DIVIDE_FACTOR;

            float newX = player.getX();
            float newY = player.getY();


            if (softMoveX(x)) {
                newX = newX + (x * Math.abs(x / 5) * BIRD_PLAYER_MOVE_SPEED);
                if (newX > 0 && newX < effectiveW) {
                    player.setX(newX);

                    if (newX > effectiveW / 2) {
                        //animatorBackground.setDuration(Double.valueOf(backgroundSceneDuration * 0.25f).longValue());
                    } else {

                    }
                }
            }

            if (softMoveY(y)) {
                //Correccion de inclinacion
                y = y - 3;

                float borderCloseFactor = Math.abs((effectiveH / 2) - Math.abs(newY - (effectiveH / 2)));
                //newY = newY + (borderCloseFactor / 10) + (y * Math.abs(y / 10) * BIRD_PLAYER_MOVE_SPEED);
                newY = newY + (y / 1.5f) * BIRD_PLAYER_MOVE_SPEED;
                if (newY > 0 && newY < effectiveH) {
                    player.setY(newY);
                }
            }
        }
    }

    private boolean softMoveX(float x) {
        return Math.abs(x) > BIRD_PLAYER_MOVE_THERESOLD;
    }

    private boolean softMoveY(float y) {
        return Math.abs(y) > BIRD_PLAYER_MOVE_THERESOLD;
    }


    /*
    private void updateBirdsPosition() {
        //Height = initialHeight - (Time^2 - (maxRange/horizontalSpeed)*Time);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Iterator<Button> iterator = birds.iterator();
                while (iterator.hasNext()) {
                    Button bird = iterator.next();
                    //bird.setX(bird.getX() - (float) (20 + (speedX * Math.random())));
                    //bird.setY(300  - ((bird.getX()/2)));

                    if (bird.getX() < -200) {
                        removeBird(bird);
                        iterator.remove();
                    }
                }
            }
        });

}
        */

    public Drawable getRandomBird() {
        Drawable result = null;
        int n = Double.valueOf(Math.floor(Math.random() * 4)).intValue();
        if (n <= BIRD_TYPES_COUNT / 2) {
            closeOpenEyes();
        }
        switch (n) {
            case 0:
                result = getResources().getDrawable(R.drawable.bird1_selector);
                break;
            case 1:
                result = getResources().getDrawable(R.drawable.bird2_selector);
                break;
            case 2:
                result = getResources().getDrawable(R.drawable.bird3_selector);
                break;
            case 3:
                result = getResources().getDrawable(R.drawable.bird4_selector);
                break;
            default:
                break;
        }

        return result;
    }

    private long mAnimationTime;

    private void stopAnimation(ValueAnimator mObjectAnimator) {
        if (mObjectAnimator != null) {
            mAnimationTime = mObjectAnimator.getCurrentPlayTime();
            mObjectAnimator.cancel();
        }
    }

    private void playAnimation(ValueAnimator mObjectAnimator) {
        if (mObjectAnimator != null) {
            mObjectAnimator.start();
            mObjectAnimator.setCurrentPlayTime(mAnimationTime);
        }
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public int getLevel() {
        return this.level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getUserId() {
        String userId = userAccountInfo.getUserNameOrFormattedAccount() + "_" + userAccountInfo.getUserAccount();  //UUID.nameUUIDFromBytes(getDeviceModel().getBytes());
        userId = userId.replaceAll("\\.", "_").replaceAll("\\#", "_").replaceAll("\\$", "_").replaceAll("\\[", "_").replaceAll("\\]", "_"); //Invalid chars '.', '#', '$', '[', or ']'
        return userId;
    }


    private class MyWorkerThread extends HandlerThread {

        private Handler mWorkerHandler;

        public MyWorkerThread(String name) {
            super(name);
        }

        public void postTask(Runnable task) {
            mWorkerHandler.post(task);
        }

        public void prepareHandler() {
            mWorkerHandler = new Handler(getLooper());
        }
    }
}
