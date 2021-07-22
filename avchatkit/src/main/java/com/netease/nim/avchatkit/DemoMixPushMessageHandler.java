package com.netease.nim.avchatkit;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import com.netease.nimlib.sdk.NimIntent;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.mixpush.MixPushMessageHandler;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by nanyi on 2020/5/19.
 */

public class DemoMixPushMessageHandler implements MixPushMessageHandler {


    /**
     * 来自系统推送唤起  2020
     */
    public static String PUSH_CONTENT_KEY = "PUSH_CONTENT_KEY";


    public DemoMixPushMessageHandler(StatusBarNotificationConfig config, Context context) {
        this.config = config;
        this.context = context;
    }

    public static final String PAYLOAD_SESSION_ID = "sessionID";
    public static final String PAYLOAD_SESSION_TYPE = "sessionType";

    // 对于华为推送，这个方法并不能保证一定会回调
    @Override
    public boolean onNotificationClicked(final Context context, Map<String, String> payload) {
        Log.i("AV_CHAT", "onNotificationClicked -------------  pushMessage payload " + payload + "     keys:" + payload.keySet());
        Log.i("fcc", "onNotificationClicked -------------  pushMessage payload " + payload + "     keys:" + payload.keySet());


        String sessionId = payload.get(PAYLOAD_SESSION_ID);
        String type = payload.get(PAYLOAD_SESSION_TYPE);
        final String content = payload.get("content");

        Intent notifyIntent = new Intent();
        notifyIntent.setComponent(initLaunchComponent(context));
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 必须

        notifyIntent.putExtra("INIT_URL_KEY", "/home?selectedTab=home");

        if (!TextUtils.isEmpty(content)) {
            notifyIntent.putExtra(PUSH_CONTENT_KEY, content);
        }
        context.startActivity(notifyIntent);
        return true;

//        if (sessionId != null && type != null) {
//            int typeValue = Integer.valueOf(type);
//            ArrayList<IMMessage> imMessages = new ArrayList<>();
//            IMMessage imMessage = MessageBuilder.createEmptyMessage(sessionId, SessionTypeEnum.typeOfValue(typeValue), 0);
//            imMessages.add(imMessage);
//            Intent notifyIntent = new Intent();
//            notifyIntent.setComponent(initLaunchComponent(context));
//            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//            notifyIntent.setAction(Intent.ACTION_VIEW);
//            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 必须
//            notifyIntent.putExtra(NimIntent.EXTRA_NOTIFY_CONTENT, imMessages);
//            context.startActivity(notifyIntent);
//            return true;
//        } else {
//            return false;
//        }

    }

    StatusBarNotificationConfig config;
    Context context;


    private ComponentName initLaunchComponent(Context context) {
        ComponentName launchComponent;
        StatusBarNotificationConfig config = this.config;
        Class<? extends Activity> entrance = config.notificationEntrance;
        if (entrance == null) {
            launchComponent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName()).getComponent();
        } else {
            launchComponent = new ComponentName(context, entrance);
        }
        return launchComponent;
    }

    // 将音视频通知 Notification 缓存，清除所有通知后再次弹出 Notification，避免清除之后找不到打开正在进行音视频通话界面的入口
    @Override
    public boolean cleanMixPushNotifications(int pushType) {
        Context context = this.context;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancelAll();
            SparseArray<Notification> nos = AVChatKit.getNotifications();
            if (nos != null) {
                int key = 0;
                for (int i = 0; i < nos.size(); i++) {
                    key = nos.keyAt(i);
                    manager.notify(key, nos.get(key));
                }
            }
        }
        return true;
    }
}