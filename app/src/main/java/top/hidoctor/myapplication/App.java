package top.hidoctor.myapplication;

import android.app.Application;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.provider.SyncStateContract;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nim.avchatkit.DemoMixPushMessageHandler;
import com.netease.nim.avchatkit.common.log.LogUtil;
import com.netease.nim.avchatkit.config.AVChatOptions;
import com.netease.nim.avchatkit.extension.CustomAttachParser;
import com.netease.nim.avchatkit.model.ITeamDataProvider;
import com.netease.nim.avchatkit.model.IUserInfoProvider;
import com.netease.nim.avchatkit.video.VideoService;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.NimStrings;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.SDKOptions;
import com.netease.nimlib.sdk.StatusBarNotificationConfig;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.LoginInfo;
import com.netease.nimlib.sdk.mixpush.NIMPushClient;
import com.netease.nimlib.sdk.msg.MessageNotifierCustomization;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.MsgServiceObserve;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.CustomNotification;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.uinfo.UserInfoProvider;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;
import com.netease.nimlib.sdk.util.NIMUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class App extends Application {
    private StatusBarNotificationConfig config;

    public StatusBarNotificationConfig getConfig() {
        return config;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initNIM();
    }

    private void initNIM() {
        VideoService.VIDEO_ON = false;
        ChatCache.setContext(this);
        // SDK初始化
        NIMClient.init(this, loginInfo(), options());



        if (NIMUtil.isMainProcess(this)) {
            initAVChatKit();
            NIMClient.toggleNotification(true);//开启通知栏消息提醒
        }

        


    }


    /**
     * 初始化AVChatkit
     */
    private void initAVChatKit() {
        AVChatKit.setContext(getApplicationContext());

        AVChatOptions avChatOptions = new AVChatOptions() {
            @Override
            public void logout(Context context) {
//                MainActivity.logout(context, true);
            }
        };
        avChatOptions.entranceActivity = MainActivity.class;
        avChatOptions.notificationIconRes = R.mipmap.ic_launcher;
        AVChatKit.init(avChatOptions);

        // 初始化日志系统
//        LogHelper.init();
        // 设置用户相关资料提供者
        AVChatKit.setUserInfoProvider(new IUserInfoProvider() {
            @Override
            public UserInfo getUserInfo(String account) {
                UserInfo userInfo = AVUserinfoProvider.getInstance().getUserinfo(account);
                if (userInfo == null) {
                    //如果本地获取不到这个人的用户资料
                }
                return userInfo;
            }

            @Override
            public String getUserDisplayName(String account) {
                UserInfo userInfo = AVUserinfoProvider.getInstance().getUserinfo(account);
                if (userInfo == null) {
                    return "未知用户";
                } else {
                    return userInfo.getName();
                }
            }
        });

        /**
         * 群组信息提供者
         */
        AVChatKit.setTeamDataProvider(new ITeamDataProvider() {
            @Override
            public String getDisplayNameWithoutMe(String teamId, String account) {
                return account; //开发者可以自行用户名展示逻辑
            }

            @Override
            public String getTeamMemberDisplayName(String teamId, String account) {
                return account; //开发者可以自行用户名展示逻辑
            }
        });
    }

    // 如果返回值为 null，则全部使用默认参数。
    private SDKOptions options() {
        SDKOptions options = new SDKOptions();

        // 如果将新消息通知提醒托管给 SDK 完成，需要添加以下配置。否则无需设置。
        config = new StatusBarNotificationConfig();
        config.notificationEntrance = MainActivity.class; // 点击通知栏跳转到该Activity

        config.notificationSmallIconId = R.mipmap.ic_launcher;
        // 呼吸灯配置
        config.ledARGB = Color.GREEN;
        config.ledOnMs = 1000;
        config.ledOffMs = 1500;
        // 通知铃声的uri字符串
        config.notificationSound = "android.resource://vip.hidoctor.purple/raw/alert";
        config.notificationFolded = false;
        options.statusBarNotificationConfig = config;
        // 配置保存图片，文件，log 等数据的目录
        // 如果 options 中没有设置这个值，SDK 会使用下面代码示例中的位置作为 SDK 的数据目录。
        // 该目录目前包含 log, file, image, audio, video, thumb 这6个目录。
        // 如果第三方 APP 需要缓存清理功能， 清理这个目录下面个子目录的内容即可。
//        String sdkPath = Environment.getExternalStorageDirectory() + "/" + getPackageName() + "/nim";

        String sdkPath = getAppCacheDir(this) + "/nim";

        options.sdkStorageRootPath = sdkPath;

        File root = new File(sdkPath + "/log");
        if (root.exists()) {
            File[] allf = root.listFiles();
            for (File f : allf) {
                Log.i("ccccc", "f:" + f.getAbsolutePath());
            }
        }


        // 配置是否需要预下载附件缩略图，默认为 true
        options.preloadAttach = true;

        // 配置附件缩略图的尺寸大小。表示向服务器请求缩略图文件的大小
        // 该值一般应根据屏幕尺寸来确定， 默认值为 Screen.width / 2
        options.thumbnailSize = 480 / 2;

        // 用户资料提供者, 目前主要用于提供用户资料，用于新消息通知栏中显示消息来源的头像和昵称
//        options.userInfoProvider = infoProvider;
        options.userInfoProvider = new UserInfoProvider() {

            @Override
            public com.netease.nimlib.sdk.uinfo.model.UserInfo getUserInfo(String account) {
                UserInfo info = new UserInfo() {
                    @Override
                    public String getAccount() {
                        return "account";
                    }

                    @Override
                    public String getName() {
                        return "name";
                    }

                    @Override
                    public String getAvatar() {
                        return "avatar";
                    }
                };
                return info;
            }

            @Override
            public String getDisplayNameForMessageNotifier(String account, String sessionId, SessionTypeEnum sessionType) {
//                return AccountUtil.getUserName(App.this);  todo  用戶名
                return "";
            }

            @Override
            public Bitmap getAvatarForMessageNotifier(SessionTypeEnum sessionType, String sessionId) {
                return null;
            }
        };


        // 定制通知栏提醒文案（可选，如果不定制将采用SDK默认文案）`
//        options.messageNotifierCustomization = messageNotifierCustomization;    // disable  2020-06-02

        return options;
    }


    // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
    private LoginInfo loginInfo() {
        String account = "60ac6fb6cdf3bd10e2b493bb"; // todo  用户账号
        String token = "7b60f7cbb06394eb65a6f9f0498099bb"; // todo 登录token

        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
            // 设置DemoCache中的account
            ChatCache.setAccount(account.toLowerCase());
            String appKey = "fd2e0e70a66a445c4e8695a958eae14a"; //todo  应用的AppKey（可选）
            if (!TextUtils.isEmpty(appKey)) {
                return new LoginInfo(account, token, appKey); // 返回LoginInfo对象
            }
            return new LoginInfo(account, token); // 返回LoginInfo对象
        } else {
            return null;
        }

    }

    /**
     * 配置 APP 保存图片/语音/文件/log等数据的目录
     * 这里示例用SD卡的应用扩展存储目录
     */
    static String getAppCacheDir(Context context) {
        String storageRootPath = null;
        try {
            // SD卡应用扩展存储区(APP卸载后，该目录下被清除，用户也可以在设置界面中手动清除)，请根据APP对数据缓存的重要性及生命周期来决定是否采用此缓存目录.
            // 该存储区在API 19以上不需要写权限，即可配置 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="18"/>
            if (context.getExternalCacheDir() != null) {
                storageRootPath = context.getExternalCacheDir().getCanonicalPath();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(storageRootPath)) {
            // SD卡应用公共存储区(APP卸载后，该目录不会被清除，下载安装APP后，缓存数据依然可以被加载。SDK默认使用此目录)，该存储区域需要写权限!
            storageRootPath = Environment.getExternalStorageDirectory() + "/" + ChatCache.getContext().getPackageName();
        }

        return storageRootPath;
    }

}
