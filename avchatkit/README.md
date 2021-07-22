# AVChatKit 使用说明

## <span id="全局配置项 AVChatOptions">全局配置项 AVChatOptions</span>

AVChatKit 组件提供了全局配置类 AVChatOptions，初始化 AVChatKit 时传入 AVChatOptions 对象。

|类型|AVChatOptions 属性|说明|
|:---|:---|:---|
|Class<? extends Activity>|entranceActivity|通知入口|
|int|notificationIconRes|通知栏icon|
|void|logout(Context context)|被踢出时，调用的方法|

## <span id="初始化">初始化</span>

在应用的 Application 的 **主进程** 中初始化 AVChatKit。

```java
AVChatOptions avChatOptions = new AVChatOptions(){
    @Override
    public void logout(Context context) {
        // 主程序登出操作
    }
};
// 点击通知栏，入口Activity
avChatOptions.entranceActivity = WelcomeActivity.class;
// 通知栏图标icon
avChatOptions.notificationIconRes = R.drawable.ic_stat_notify_msg;
// 初始化 AVChatKit
AVChatKit.init(avChatOptions);
```

- 示例

```java
AVChatOptions avChatOptions = new AVChatOptions(){
    @Override
    public void logout(Context context) {
        MainActivity.logout(context, true);
    }
};
avChatOptions.entranceActivity = WelcomeActivity.class;
avChatOptions.notificationIconRes = R.drawable.ic_stat_notify_msg;
AVChatKit.init(avChatOptions);

// 初始化日志系统
LogHelper.init();
// 设置用户相关资料提供者
AVChatKit.setUserInfoProvider(new IUserInfoProvider() {
    @Override
    public UserInfo getUserInfo(String account) {
        return NimUIKit.getUserInfoProvider().getUserInfo(account);
    }

    @Override
    public String getUserDisplayName(String account) {
        return UserInfoHelper.getUserDisplayName(account);
    }
});
// 设置群组数据提供者
AVChatKit.setTeamDataProvider(new ITeamDataProvider() {
    @Override
    public String getDisplayNameWithoutMe(String teamId, String account) {
        return TeamHelper.getDisplayNameWithoutMe(teamId, account);
    }

    @Override
    public String getTeamMemberDisplayName(String teamId, String account) {
        return TeamHelper.getTeamMemberDisplayName(teamId, account);
    }
});
```

AVChatKit 中用到的 Activity 已经在 AVChatKit 工程的 AndroidManifest.xml 文件中注册好，上层 APP 无需再去添加注册。

## <span id="快速使用">快速使用</span>

### <span id="发起点对点音视频通话呼叫">发起点对点音视频通话呼叫</span>

- API 原型

```java
/**
 * 发起音视频通话呼叫
 * @param context   上下文
 * @param account   被叫方账号
 * @param displayName   被叫方显示名称
 * @param callType      音视频呼叫类型
 * @param source        发起呼叫的来源，参考AVChatActivityEx.FROM_INTERNAL/FROM_BROADCASTRECEIVER
 */
public static void outgoingCall(Context context, String account, String displayName, int callType, int source);
```

- 参数介绍

|参数|说明|
|:---|:---|
|context   |上下文|
|account   |被叫方账号|
|displayName   |被叫方显示名称|
|callType      |音视频呼叫类型|
|source        |发起呼叫的来源，参考AVChatActivityEx.FROM_INTERNAL/FROM_BROADCASTRECEIVER|

- 示例

```java
AVChatKit.outgoingCall(context, "testAccount", "displayName" AVChatType.AUDIO, AVChatActivity.FROM_INTERNAL);
```

### <span id="发起群组音视频通话呼叫">发起群组音视频通话呼叫</span>

-  API 原型

```java
/**
 * 发起群组音视频通话呼叫
 * @param context   上下文
 * @param receivedCall  是否是接收到的来电
 * @param teamId    team id
 * @param roomId    音视频通话room id
 * @param accounts  音视频通话账号集合
 * @param teamName  群组名称
 */
public static void outgoingTeamCall(Context context, boolean receivedCall, String teamId, String roomId, ArrayList<String> accounts, String teamName);
```

- 参数说明

|参数|说明|
|:---|:---|
|context   |上下文|
|receivedCall  |是否是接收到的来电|
|teamId    |team id|
|roomId    |音视频通话 room id|
|accounts  |音视频通话账号集合|
|teamName  |群组名称|

- 示例

```java
// 以下参数为示例
AVChatKit.outgoingTeamCall(context, false, "1111", "roomName", accounts, "teamName");
```

### <span id="打开网络通话设置界面">打开网络通话设置界面</span>

- API 原型

```java
/**
 * 打开网络通话设置界面
 * @param context   上下文
 */
public static void startAVChatSettings(Context context);
```

- 示例

```java
AVChatKit.startAVChatSettings(SettingsActivity.this);
```







云上妇幼Android 云信对接
1、导入avchatKit库，并引入到主项目中
2、打开 app/src/main/AndroidManifest.xml 文件，添加必要的设备权限。
例如：
 <uses-permission android:name="android.permission.INTERNET"/>
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
 <uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
 <uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
 <uses-permission android:name="android.permission.WAKE_LOCK"/>
 <uses-permission android:name="android.permission.CAMERA"/>
 <uses-permission android:name="android.permission.RECORD_AUDIO"/>
 <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS"/>
 <uses-permission android:name="android.permission.BLUETOOTH"/>
 <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
 <uses-permission android:name="android.permission.BROADCAST_STICKY"/>
 <uses-permission android:name="android.permission.READ_PHONE_STATE"/>

 <uses-feature android:name="android.hardware.camera"/>
 <uses-feature android:name="android.hardware.camera.autofocus"/>

  <!-- 创建悬浮框需要的权限 -->
 <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
 <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />


3、Application 中初始化 云信音视频   详情可参照 云信Android初始化

	代码如下：

	public void onCreate() {
		 VideoService.VIDEO_ON = false;
		 ChatCache.setContext(this);
		 // SDK初始化
		 NIMClient.init(this, loginInfo(), options());
		 if (NIMUtil.isMainProcess(this)) {
				NIMClient.toggleNotification(true);//开启通知栏消息提醒
				initAVChatKit();

			}
	}




	   // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
    private LoginInfo loginInfo() {
        String account = AccountUtil.getNimAccid(this);
        String token = AccountUtil.getNimToken(this);
        Log.i(Constants.AV_CHAT, "app startup   ---> loginInfo   account: "+account+"    token:"+token );

        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
            // 设置DemoCache中的account
            ChatCache.setAccount(account.toLowerCase());
            return new LoginInfo(account, token); // 返回LoginInfo对象
        } else {
            return null;
        }
    }

	 // 如果返回值为 null，则全部使用默认参数。
    private SDKOptions options() {
        SDKOptions options = new SDKOptions();
        if(!TextUtils.isEmpty(AccountUtil.getAppkey(this))){
            options.appKey = AccountUtil.getAppkey(this);
        }

        // 如果将新消息通知提醒托管给 SDK 完成，需要添加以下配置。否则无需设置。
        StatusBarNotificationConfig config = new StatusBarNotificationConfig();
//        config.notificationEntrance = SplashActivity.class; // 点击通知栏跳转到该Activity
        config.notificationEntrance = MainHtmlActivity.class; // 点击通知栏跳转到该Activity

        config.notificationSmallIconId = R.mipmap.ic_launcher;
        // 呼吸灯配置
        config.ledARGB = Color.GREEN;
        config.ledOnMs = 1000;
        config.ledOffMs = 1500;
        // 通知铃声的uri字符串
//        config.notificationSound = "android.resource://cc.ewell.intelligentcinsultation/raw/msg";
//        config.notificationSound = "android.resource://" + getPackageName() + "/" + R.raw.msg;
        config.notificationSound = "android.resource://com.netease.nim.demo/raw/msg";
        config.notificationFolded = false;
        options.statusBarNotificationConfig = config;
        // 配置保存图片，文件，log 等数据的目录
        // 如果 options 中没有设置这个值，SDK 会使用下面代码示例中的位置作为 SDK 的数据目录。
        // 该目录目前包含 log, file, image, audio, video, thumb 这6个目录。
        // 如果第三方 APP 需要缓存清理功能， 清理这个目录下面个子目录的内容即可。
//        String sdkPath = Environment.getExternalStorageDirectory() + "/" + getPackageName() + "/nim";
        String sdkPath = getAppCacheDir(this) + "/nim";
        options.sdkStorageRootPath = sdkPath;

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
                return AccountUtil.getUserName(EApplication.this);
            }

            @Override
            public Bitmap getAvatarForMessageNotifier(SessionTypeEnum sessionType, String sessionId) {
                return null;
            }
        };
        // 定制通知栏提醒文案（可选，如果不定制将采用SDK默认文案）`
        options.messageNotifierCustomization = messageNotifierCustomization;
        return options;
    }


	 private MessageNotifierCustomization messageNotifierCustomization = new MessageNotifierCustomization() {
        @Override
        public String makeNotifyContent(String nick, IMMessage message) {
            Map<String, Object> pushPayload = message.getPushPayload();
            String content = pushPayload.get("content").toString();
            LogUtil.i(Constants.AV_CHAT,"--makeNotifyContent--content:"+content);
            PushBean bean = new Gson().fromJson(content, PushBean.class);
            if(EApplication.onNotificationPop!=null){
                EApplication.onNotificationPop.run();
            }
            return NimStrings.DEFAULT.status_bar_custom_message = bean.getContent(); // 采用SDK默认文案
        }

        @Override
        public String makeTicker(String nick, IMMessage message) {
            Map<String, Object> pushPayload = message.getPushPayload();
            String content = pushPayload.get("content").toString();
            LogUtil.i(Constants.AV_CHAT,"--makeTicker--content:"+content);
            PushBean bean = new Gson().fromJson(content, PushBean.class);
            return NimStrings.DEFAULT.status_bar_custom_message = bean.getContent();
        }

        @Override
        public String makeRevokeMsgTip(String revokeAccount, IMMessage item) {
            return null;
        }
    };

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




	/**
     * 初始化AVChatkit
     */
    private void initAVChatKit() {
        AVChatKit.setContext(getApplicationContext());

        AVChatOptions avChatOptions = new AVChatOptions(){
            @Override
            public void logout(Context context) {
//                MainActivity.logout(context, true);
            }
        };
        avChatOptions.entranceActivity = SplashActivity.class;
        avChatOptions.notificationIconRes = R.mipmap.ic_launcher;
        AVChatKit.init(avChatOptions);

        // 初始化日志系统
//        LogHelper.init();
        // 设置用户相关资料提供者
        AVChatKit.setUserInfoProvider(new IUserInfoProvider() {
            @Override
            public UserInfo getUserInfo(String account) {
                UserInfo userInfo =  AVUserinfoProvider.getInstance().getUserinfo(account);
                if (userInfo == null) {
                    //如果本地获取不到这个人的用户资料
                }
                return userInfo;
            }

            @Override
            public String getUserDisplayName(String account) {
                UserInfo userInfo =  AVUserinfoProvider.getInstance().getUserinfo(account);
                if (userInfo == null) {
                    return "未知用户";
                }else {
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
4、云信视频聊天调用

   在需要调用云信音视频的地方 -->首先进行权限判断
   代码如下： 使用云信自带权限判断 （使用其他权限判断也可以 ）
    private static final int BASIC_PERMISSION_REQUEST_CODE = 0x100;

    private void checkPermission() {
        List<String> lackPermissions = AVChatManager.getInstance().checkPermission(MainHtmlActivity.this);
        if (lackPermissions.isEmpty()) {
            onBasicPermissionSuccess();
        } else {
            String[] permissions = new String[lackPermissions.size()];
            for (int i = 0; i < lackPermissions.size(); i++) {
                permissions[i] = lackPermissions.get(i);
            }
            MPermission.with(MainHtmlActivity.this)
                    .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                    .permissions(permissions)
                    .request();
        }
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        onPermissionChecked();
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        Toast.makeText(this, "音视频通话所需权限未全部授权，部分功能可能无法正常运行！", Toast.LENGTH_SHORT).show();
        onPermissionChecked();
    }

    private void onPermissionChecked() {
        startVideo(); // 启动音视频
    }



	启动云信音视频：


   private void startVideo() {
        Map<String, String> map = dataMap;

        if (creator.equals(userCode)) {//发起者
            Log.i("TeamAVChat", "startAVChat");
			// roomName 房间号  accounts 房间成员
            AVChatManager.getInstance().createRoom(roomName, null, new AVChatCallback<AVChatChannelInfo>() {
            @Override
            public void onSuccess(AVChatChannelInfo avChatChannelInfo) {
                TeamAVChatProfile.sharedInstance().setTeamAVChatting(true);
                AVChatKit.outgoingTeamCall(MainHtmlActivity.this, false,
                        "", roomName, accounts, "");
            }

            @Override
            public void onFailed(int i) {
                Log.e(Constants.AV_CHAT, "onFailed: " + i);
                if (i == 417) {
                    //重新进入
                    AVChatKit.outgoingTeamCall(MainHtmlActivity.this, false,
                            "", roomName, accounts, "");
                }
            }

            @Override
            public void onException(Throwable throwable) {
                Log.e(Constants.AV_CHAT, "onException: " + throwable.getMessage());
                throwable.printStackTrace();
            }
        });
        } else {//加入这
            Log.i("TeamAVChat", "joinRoom");
			AVChatKit.outgoingTeamCall(MainHtmlActivity.this, false,
                "", roomName, accounts, "");
        }
    }















