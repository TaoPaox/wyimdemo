package com.netease.nim.avchatkit.video;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;

/**
 * Created by nanyi on 2019/9/23.
 */

public class PermissionUtil {

    public static boolean hasOverlayPermission(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(c);
        } else {
            return true;
        }
    }

}
