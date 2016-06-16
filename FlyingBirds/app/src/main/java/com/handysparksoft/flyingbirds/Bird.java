package com.handysparksoft.flyingbirds;

import android.content.Context;
import android.widget.Button;

/**
 * Created by davasens on 9/28/2015.
 */
public class Bird extends Button {
    private float initialPositionX;
    private float initialPositionY;
    private Boolean hasCrashed = false;

    public Bird(Context context) {
        super(context);
    }



    public float getInitialPositionY() {
        return initialPositionY;
    }

    public void setInitialPositionY(float initialPositionY) {
        this.initialPositionY = initialPositionY;
    }

    public float getInitialPositionX() {
        return initialPositionX;
    }

    public void setInitialPositionX(float initialPositionX) {
        this.initialPositionX = initialPositionX;
    }

    public Boolean getHasCrashed() {
        return hasCrashed;
    }

    public void setHasCrashed(Boolean hasCrashed) {
        this.hasCrashed = hasCrashed;
    }
}
