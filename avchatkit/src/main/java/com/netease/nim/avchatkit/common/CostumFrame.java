package com.netease.nim.avchatkit.common;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by nanyi on 2019/9/23.
 */

public class CostumFrame extends FrameLayout {

    public CostumFrame(@NonNull Context context) {
        super(context);
    }

    public CostumFrame(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CostumFrame(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(interceptOn)
        return true;
        return super.onInterceptTouchEvent(ev);
    }

    private boolean interceptOn  = false ;


    public void setInterceptOn(boolean interceptOn) {
        this.interceptOn = interceptOn;
    }
}
