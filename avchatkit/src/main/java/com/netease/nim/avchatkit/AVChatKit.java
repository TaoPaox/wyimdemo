package com.netease.nim.avchatkit;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Toast;

import com.netease.nim.avchatkit.activity.AVChatActivity;
import com.netease.nim.avchatkit.activity.AVChatSettingsActivity;
import com.netease.nim.avchatkit.common.dialog.DialogMaker;
import com.netease.nim.avchatkit.common.log.ILogUtil;
import com.netease.nim.avchatkit.common.log.LogUtil;
import com.netease.nim.avchatkit.config.AVChatOptions;
import com.netease.nim.avchatkit.constant.AVChatExitCode;
import com.netease.nim.avchatkit.constant.CallStateEnum;
import com.netease.nim.avchatkit.controll.AVChatController;
import com.netease.nim.avchatkit.model.ITeamDataProvider;
import com.netease.nim.avchatkit.model.IUserInfoProvider;
import com.netease.nim.avchatkit.module.AVChatControllerCallback;
import com.netease.nim.avchatkit.receiver.PhoneCallStateObserver;
import com.netease.nim.avchatkit.teamavchat.activity.TeamAVChatActivity;
import com.netease.nim.avchatkit.video.VideoService;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatControlCommand;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.model.AVChatChannelInfo;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatNotifyOption;

import java.util.ArrayList;

/**
 * 云信音视频组件定制化入口
 * Created by winnie on 2017/12/6.
 */

public class AVChatKit {

    private static final String TAG = AVChatKit.class.getSimpleName();

    private static Context context;

    private static String account;

    private static boolean mainTaskLaunching;

    private static AVChatOptions avChatOptions;

    private static IUserInfoProvider userInfoProvider;

    private static ITeamDataProvider teamDataProvider;

    private static ILogUtil iLogUtil;

    private static SparseArray<Notification> notifications = new SparseArray<>();

    public static void init(AVChatOptions avChatOptions) {
        AVChatKit.avChatOptions = avChatOptions;
        registerAVChatIncomingCallObserver(true);
    }

    public static void setContext(Context context) {
        AVChatKit.context = context;
    }

    public static Context getContext() {
        return context;
    }

    public static String getAccount() {
        return account;
    }

    public static void setAccount(String account) {
        AVChatKit.account = account;
    }

    public static void setMainTaskLaunching(boolean mainTaskLaunching) {
        AVChatKit.mainTaskLaunching = mainTaskLaunching;
    }

    public static boolean isMainTaskLaunching() {
        return mainTaskLaunching;
    }

    /**
     * 获取通知栏提醒数组
     */
    public static SparseArray<Notification> getNotifications() {
        return notifications;
    }

    /**
     * 获取音视频初始化配置
     *
     * @return AVChatOptions
     */
    public static AVChatOptions getAvChatOptions() {
        return avChatOptions;
    }

    /**
     * 设置用户相关资料提供者
     *
     * @param userInfoProvider 用户相关资料提供者
     */
    public static void setUserInfoProvider(IUserInfoProvider userInfoProvider) {
        AVChatKit.userInfoProvider = userInfoProvider;
    }

    /**
     * 获取用户相关资料提供者
     *
     * @return IUserInfoProvider
     */
    public static IUserInfoProvider getUserInfoProvider() {
        return userInfoProvider;
    }

    /**
     * 获取日志系统接口
     *
     * @return ILogUtil
     */
    public static ILogUtil getiLogUtil() {
        return iLogUtil;
    }

    /**
     * 设置日志系统接口
     *
     * @param iLogUtil 日志系统接口
     */
    public static void setiLogUtil(ILogUtil iLogUtil) {
        AVChatKit.iLogUtil = iLogUtil;
    }

    /**
     * 设置群组数据提供者
     *
     * @param teamDataProvider 群组数据提供者
     */
    public static void setTeamDataProvider(ITeamDataProvider teamDataProvider) {
        AVChatKit.teamDataProvider = teamDataProvider;
    }

    /**
     * 获取群组数据提供者
     *
     * @return ITeamDataProvider
     */
    public static ITeamDataProvider getTeamDataProvider() {
        return teamDataProvider;
    }

    /**
     * 发起音视频通话呼叫
     *
     * @param context     上下文
     * @param account     被叫方账号
     * @param displayName 被叫方显示名称
     * @param callType    音视频呼叫类型
     * @param source      发起呼叫的来源，参考AVChatActivityEx.FROM_INTERNAL/FROM_BROADCASTRECEIVER
     */
    public static void outgoingCall(final Context context, String account, String displayName, final int callType, int source) {

        /**
         *    nim demo way of p2p call
         */
        AVChatActivity.outgoingCall(context, account, displayName, callType, source);
        if (true) return;


        /**
         *   use controller --> this will occupy resource during shadow p2p call
         */
//        controller = new AVChatController(context, null);
//        controller.doCalling(account, AVChatType.typeOfValue(callType), new AVChatControllerCallback<AVChatData>() {
//            @Override
//            public void onSuccess(AVChatData avChatData) {
//                startAVChat(""+avChatData.getChatId() , context );
//                hangUpShadowCall();
//            }
//
//            @Override
//            public void onFailed(int code, String errorMsg) {
//                Log.i("AV_CHAT","outgoingCall   onFailed:"+errorMsg+"   code:"+code);
//            }
//        });


        /**
         *   use controller --> this will throw error ...
         */


        AVChatManager.getInstance().enableRtc();
        AVChatNotifyOption notifyOption = new AVChatNotifyOption();
        notifyOption.extendMessage = "extra_data";
        notifyOption.forceKeepCalling = false;

        AVChatManager.getInstance().call2(account, AVChatType.typeOfValue(callType), notifyOption, new AVChatCallback<AVChatData>() {

            @Override
            public void onSuccess(AVChatData data) {
                Log.i("AV_CHAT", "call2   onSuccess:" + data.getChatId());
                channelId = data.getChatId();
                startAVChat("" + data.getChatId(), context, callType);
            }

            @Override
            public void onFailed(int code) {
                Log.i("AV_CHAT", "call2   code:" + code);

                if (code == ResponseCode.RES_FORBIDDEN) {
                    Toast.makeText(getContext(), R.string.avchat_no_permission, Toast.LENGTH_SHORT).show();
                } else if (code == 11001) {
                    Toast.makeText(getContext(), "被叫不在线", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), R.string.avchat_call_failed, Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onException(Throwable exception) {
                LogUtil.d(TAG, "avChat call onException->" + exception);
                Log.i("AV_CHAT", "call2   onException:" + Log.getStackTraceString(exception));

            }
        });

    }


    static AVChatController controller;

    static long channelId;

    public static void hangUpShadowCall() {
        if (controller != null) {
            controller.hangUp(AVChatExitCode.HANGUP);
        }
        AVChatManager.getInstance().disableRtc();
        AVChatManager.getInstance().hangUp2(channelId, new AVChatCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
            }

            @Override
            public void onFailed(int code) {
            }

            @Override
            public void onException(Throwable exception) {
            }
        });

    }


    private static void startAVChat(final String roomName, final Context c, final int avChatType) {
        AVChatManager.getInstance().createRoom(roomName, null, new AVChatCallback<AVChatChannelInfo>() {
            @Override
            public void onSuccess(AVChatChannelInfo avChatChannelInfo) {
                Log.i("AV_CHAT", "startAVChat   onSuccess:" + roomName);
                TeamAVChatProfile.sharedInstance().setTeamAVChatting(true);
                AVChatKit.outgoingTeamCall(c, false,
                        "", roomName, new ArrayList<String>(), "", avChatType);
            }

            @Override
            public void onFailed(int i) {
                Log.i("AV_CHAT", "startAVChat   onFailed:" + i);
                if (i == 417) {
                    //重新进入
                    AVChatKit.outgoingTeamCall(c, false,
                            "", roomName, new ArrayList<String>(), "", avChatType);
                } else {
                    Toast.makeText(getContext(), R.string.avchat_call_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }


    /**
     * 发起群组音视频通话呼叫
     *
     * @param context      上下文
     * @param receivedCall 是否是接收到的来电
     * @param teamId       team id
     * @param roomId       音视频通话room id
     * @param accounts     音视频通话账号集合
     * @param teamName     群组名称
     */
    public static void outgoingTeamCall(Context context, boolean receivedCall, String teamId, String roomId, ArrayList<String> accounts, String teamName, int callType) {
        TeamAVChatActivity.startActivity(context, receivedCall, teamId, roomId, accounts, teamName);
//        VideoService.startService(context, receivedCall, teamId, roomId, accounts, teamName, callType);
    }

    /**
     * 打开网络通话设置界面
     *
     * @param context 上下文
     */
    public static void startAVChatSettings(Context context) {
        context.startActivity(new Intent(context, AVChatSettingsActivity.class));
    }

    /**
     * 注册音视频来电观察者
     *
     * @param register 注册或注销
     */
    private static void registerAVChatIncomingCallObserver(boolean register) {
        AVChatManager.getInstance().observeIncomingCall(inComingCallObserver, register);
    }

    private static Observer<AVChatData> inComingCallObserver = new Observer<AVChatData>() {
        @Override
        public void onEvent(final AVChatData data) {
            String extra = data.getExtra();
            Log.i("Extra", "inComingCallObserver   Extra Message->" + extra);
            Log.i("AV_CHAT", "inComingCallObserver   Extra Message->" + extra);
            if (PhoneCallStateObserver.getInstance().getPhoneCallState() != PhoneCallStateObserver.PhoneCallStateEnum.IDLE
                    || AVChatProfile.getInstance().isAVChatting()
                    || TeamAVChatProfile.sharedInstance().isTeamAVChatting()
                    || AVChatManager.getInstance().getCurrentChatId() != 0) {
                LogUtil.i(TAG, "reject incoming call data =" + data.toString() + " as local phone is not idle");
                AVChatManager.getInstance().sendControlCommand(data.getChatId(), AVChatControlCommand.BUSY, null);
                return;
            }
            // 有网络来电打开AVChatActivity
            AVChatProfile.getInstance().setAVChatting(true);
            AVChatProfile.getInstance().launchActivity(data, userInfoProvider.getUserDisplayName(data.getAccount()), AVChatActivity.FROM_BROADCASTRECEIVER);
        }
    };

}
