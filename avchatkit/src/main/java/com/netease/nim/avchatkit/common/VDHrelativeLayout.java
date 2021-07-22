package com.netease.nim.avchatkit.common;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

/**
 * Created by nanyi on 2019/9/23.
 */


public class VDHrelativeLayout extends RelativeLayout {

    private static final String TAG="VDHrelativeLayout";

    private static final class ViewOffsetWrapper {
        private ViewOffsetWrapper(View view) {
            this.view = view;
        }

        View view;
        int xOffest;
        int yOffset;

        private void recordOffset(){
            xOffest = view.getLeft();
            yOffset = view.getTop();
        }

        private void applyRecordedOffset(){
            view.setLeft(0);
            view.setTop(0);
            view.offsetLeftAndRight(xOffest);
            view.offsetTopAndBottom(yOffset);

            //  below code does not work with  JABBAR VIDEO sdk??  TODO why ?? what's the difference ??

//            view.setLeft(xOffest);
//            view.setTop(xOffest);

        }
    }

    public VDHrelativeLayout(Context context) {
        this(context,null);
    }

    public VDHrelativeLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public VDHrelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private ViewDragHelper mDragger;


    private String currentTag;

    private void init(){

        viewArrayMap=new ArrayMap<>();

        mDragger = ViewDragHelper.create(this, 1000f, new ViewDragHelper.Callback()
        {
            @Override
            public boolean tryCaptureView(View child, int pointerId)
            {
                Object tag = child.getTag();
                if(tag instanceof String ) {

                    String cast = (String) tag;
                    if(cast.endsWith("_draggable")){
                        currentTag=cast;

                        if(viewArrayMap.containsKey(cast)){

                        } else {
                            viewArrayMap.put(cast,new ViewOffsetWrapper(child));
                        }
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx)
            {
                return left;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy)
            {
                return top;
            }
        });

    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_CANCEL
                || action == MotionEvent.ACTION_UP) {
            mDragger.cancel();
            return false;
        }

        return mDragger.shouldInterceptTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        mDragger.processTouchEvent(event);
//        if(mVdhView!=null){
//            mVdhXOffset = mVdhView.getLeft();
//            mVdhYOffset = mVdhView.getTop();
//        }
        if(currentTag!=null){
            viewArrayMap.get(currentTag).recordOffset();
        }
        return true;
    }



//    View mVdhView;
//    int mVdhXOffset;
//    int mVdhYOffset;

    ArrayMap<String, ViewOffsetWrapper> viewArrayMap;

    @Override
    public void computeScroll() {
        if (mDragger.continueSettling(true)) {
            postInvalidate();
        } else {
//            if(mVdhView!=null){
//                mVdhXOffset = mVdhView.getLeft();
//                mVdhYOffset = mVdhView.getTop();
//            }
            if(currentTag!=null){
                viewArrayMap.get(currentTag).recordOffset();
            }

        }
    }



    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        // Reapply VDH offsets
//        if(mVdhView!=null) {
//            mVdhView.offsetLeftAndRight(mVdhXOffset);
//            mVdhView.offsetTopAndBottom(mVdhYOffset);
//        }
        if(currentTag!=null){
            viewArrayMap.get(currentTag).applyRecordedOffset();
        }

    }


    public void resetPosition(String key){
        ViewOffsetWrapper wrapper = viewArrayMap.get(key);
        if(wrapper!=null) {
            wrapper.yOffset=0;
            wrapper.xOffest=0;
            wrapper.view.requestLayout();
        }
    }

}
