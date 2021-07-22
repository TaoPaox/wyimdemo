package top.hidoctor.myapplication;

import android.content.Context;

import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;

public class ChatCache {

    private static Context context;

    private static String account;

    private static StatusBarNotificationConfig notificationConfig;

    public static void clear() {
        account = null;
    }

    public static String getAccount() {
        return account;
    }

    private static boolean mainTaskLaunching;

    public static void setAccount(String account) {
        ChatCache.account = account;
//        NimUIKit.setAccount(account);
        AVChatKit.setAccount(account);
//        RTSKit.setAccount(account);
    }

    public static void setNotificationConfig(StatusBarNotificationConfig notificationConfig) {
       ChatCache.notificationConfig = notificationConfig;
    }

    public static StatusBarNotificationConfig getNotificationConfig() {
        return notificationConfig;
    }

    public static Context getContext() {
        return context;
    }

    public static void setContext(Context context) {
       ChatCache.context = context.getApplicationContext();

        AVChatKit.setContext(context);
//        RTSKit.setContext(context);
    }

    public static void setMainTaskLaunching(boolean mainTaskLaunching) {
        ChatCache.mainTaskLaunching = mainTaskLaunching;

        AVChatKit.setMainTaskLaunching(mainTaskLaunching);
    }

    public static boolean isMainTaskLaunching() {
        return mainTaskLaunching;
    }
}
