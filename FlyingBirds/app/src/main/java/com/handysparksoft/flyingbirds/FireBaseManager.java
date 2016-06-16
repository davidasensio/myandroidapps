package com.handysparksoft.flyingbirds;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by davasens on 3/14/2015.
 */
public class FireBaseManager {
    private final static String LOG_TAG = FireBaseManager.class.getSimpleName();
    private final static String FIREBASE_URL_FLYING_BIRDS = "https://flyingbirds.firebaseio.com/";
    //private final static String FIREBASE_URL_SENKU_SCORES = "https://senku.firebaseio.com/scores/";

    Firebase rootRef;
    private Context context;
    private boolean initialized = false;

    public FireBaseManager(Context context) {
        this.context = context;
        Firebase.setAndroidContext(context);
    }

    public void initFireBase() {
        if (!initialized) {
            init();
            initialized = true;
        }
    }

    private void init() {
        rootRef = new Firebase(FIREBASE_URL_FLYING_BIRDS);

        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                //Log.d(LOG_TAG,snapshot.getValue());
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.d(LOG_TAG, "The read failed: " + firebaseError.getMessage());
            }
        });
    }

    private SharedPreferences getPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void storeUserDataInFireBase(String userId, Map<String, String> mapData) {
        if (rootRef == null) {
            init();
        }

        String userPath = "user/" + userId + "/";

        if (mapData != null && mapData.keySet().size() > 0) {
            Iterator<String> iterator = mapData.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                String value = mapData.get(key);

                rootRef.child(userPath + key).setValue(value);
            }
        }

        //rootRef.updateChildren();
    }

    public void storeUserScoreInFireBase(String userAccount, String maxScore) {
        if (rootRef == null) {
            init();
        }
        rootRef.child("user/scores/" + userAccount).setValue(maxScore);
        //rootRef.updateChildren();
    }

    public void syncUserNickname(String userId) {
        final Context ctx = this.context;

        String localUserNickname = getPrefs().getString("nickname", null);


        Firebase userData = new Firebase(FIREBASE_URL_FLYING_BIRDS);

        final String userPath = "user/" + userId;
        //Query queryRef = scoresRef.orderByValue();
        userData.child(userPath + "/test").setValue("0");
        userData.addListenerForSingleValueEvent(new ValueEventListener() {
            //scoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Object object = snapshot.getValue();
                if (object != null) {
                    Map<String, Object> userMap = (Map<String, Object>) snapshot.child(userPath).getValue();
                    String fireBaseUserNickname = (String) userMap.get("nickname");

                    if (fireBaseUserNickname != null && fireBaseUserNickname.length() > 0) {
                        PreferenceManager.getDefaultSharedPreferences(context).edit().putString("nickname", fireBaseUserNickname);
                    }
                }
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        });
    }

    public void storeUserNickname(String userId, String userNickname) {
        if (rootRef == null) {
            init();
        }

        String userPath = "user/" + userId + "/";
        rootRef.child(userPath + "nickname").setValue(userNickname);
    }

    public void getUsersScoreFromFireBase() {
        final Context ctx = this.context;

        Firebase scoresRef = new Firebase(FIREBASE_URL_FLYING_BIRDS);

        //Query queryRef = scoresRef.orderByValue();
        scoresRef.child("user/scores/test").setValue("0");
        scoresRef.addListenerForSingleValueEvent(new ValueEventListener() {
            //scoresRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ArrayList<String> scores = new ArrayList<String>();

                Object object = snapshot.getValue();
                if (object != null) {
                    Map<String, Object> scoresMap = (Map<String, Object>) snapshot.child("user/scores/").getValue();
                    Iterator<String> it = scoresMap.keySet().iterator();
                    while (it.hasNext()) {
                        String key = it.next();
                        String value = (String) scoresMap.get(key);

                        if (!key.equals("test")) {
                            scores.add(value + " - " + key);
                        }
                    }
                    Collections.sort(scores);
                    Collections.reverse(scores);
                }
                Log.d(LOG_TAG, object.toString());

                Bundle bundle = new Bundle();
                bundle.putSerializable("array_list", scores);

                /*
                Intent intent = new Intent(ctx, ScoreScreen.class);
                intent.putExtras(bundle);
                ctx.startActivity(intent);
                */

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                //Log.d(LOG_TAG,"The read failed: " + firebaseError.getMessage());
            }
        });

        scoresRef.child("user/scores/test").removeValue();
    }


}
