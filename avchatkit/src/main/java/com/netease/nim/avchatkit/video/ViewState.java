package com.netease.nim.avchatkit.video;

import android.content.pm.ActivityInfo;
import android.view.WindowManager;

import static com.netease.nim.avchatkit.video.CallViewState.FULL;
import static com.netease.nim.avchatkit.video.CallViewState.SMALL;

/**
 * Created by nanyi on 2019/9/23.
 */

public interface ViewState {


    WindowManager.LayoutParams getRootLayout();
    CallViewState getCallViewState();

    class ViewStateFull implements ViewState {

        WindowManager.LayoutParams layoutParams;

        public ViewStateFull(WindowManager.LayoutParams layoutParams) {
            this.layoutParams = layoutParams;
        }

        @Override
        public WindowManager.LayoutParams getRootLayout() {
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.x = 0;
            layoutParams.y = 0;
            return layoutParams;

        }

        @Override
        public CallViewState getCallViewState() {
            return FULL;
        }

    }

    class ViewStateSmall implements ViewState {

        static int WIDTH = 200;
        static int HEIGHT = 300;

        WindowManager.LayoutParams layoutParams;

        public ViewStateSmall(WindowManager.LayoutParams layoutParams) {
            this.layoutParams = layoutParams;
        }

        @Override
        public WindowManager.LayoutParams getRootLayout() {
            layoutParams.width = WIDTH;
            layoutParams.height = HEIGHT;
            return layoutParams;
        }

        @Override
        public CallViewState getCallViewState() {
            return SMALL;
        }

    }



}
