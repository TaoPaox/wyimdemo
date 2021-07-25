package com.netease.nim.avchatkit.video;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nim.avchatkit.R;
import com.netease.nim.avchatkit.TeamAVChatProfile;
import com.netease.nim.avchatkit.common.CostumFrame;
import com.netease.nim.avchatkit.common.log.LogUtil;
import com.netease.nim.avchatkit.common.permission.annotation.OnMPermissionGranted;
import com.netease.nim.avchatkit.common.recyclerview.decoration.SpacingDecoration;
import com.netease.nim.avchatkit.common.util.ScreenUtil;
import com.netease.nim.avchatkit.controll.AVChatSoundPlayer;
import com.netease.nim.avchatkit.teamavchat.TeamAVChatNotification;
import com.netease.nim.avchatkit.teamavchat.TeamAVChatVoiceMuteDialog;
import com.netease.nim.avchatkit.teamavchat.activity.TeamAVChatActivity;
import com.netease.nim.avchatkit.teamavchat.adapter.TeamAVChatAdapter;
import com.netease.nim.avchatkit.teamavchat.module.SimpleAVChatStateObserver;
import com.netease.nim.avchatkit.teamavchat.module.TeamAVChatItem;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.AuthServiceObserver;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.AVChatStateObserver;
import com.netease.nimlib.sdk.avchat.constant.AVChatControlCommand;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.constant.AVChatUserRole;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoCropRatio;
import com.netease.nimlib.sdk.avchat.constant.AVChatVideoScalingType;
import com.netease.nimlib.sdk.avchat.model.AVChatAudioFrame;
import com.netease.nimlib.sdk.avchat.model.AVChatControlEvent;
import com.netease.nimlib.sdk.avchat.model.AVChatData;
import com.netease.nimlib.sdk.avchat.model.AVChatParameters;
import com.netease.nimlib.sdk.avchat.model.AVChatVideoFrame;
import com.netease.nimlib.sdk.avchat.video.AVChatCameraCapturer;
import com.netease.nimlib.sdk.avchat.video.AVChatTextureViewRenderer;
import com.netease.nimlib.sdk.avchat.video.AVChatVideoCapturerFactory;
import com.netease.nrtc.video.render.IVideoRender;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import static com.netease.nim.avchatkit.teamavchat.module.TeamAVChatItem.TYPE.TYPE_DATA;


public class VideoService extends Service {

    public static void startService(Context context, boolean receivedCall, String teamId, String roomId, ArrayList<String> accounts, String teamName, int callType) {

        Log.i(TAG, "---startService----");

        if (VIDEO_ON) {
            Toast.makeText(context, "正在视频通话中！", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PermissionUtil.hasOverlayPermission(context)) {

            Toast.makeText(context, "请授予显示悬浮窗权限", Toast.LENGTH_SHORT).show();

            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);   // ACTION_MANAGE_OVERLAY_PERMISSION    ACTION_APPLICATION_DETAILS_SETTINGS
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            } catch (Exception e) {

            }

        } else {

            Intent intent = new Intent();
            intent.setClass(context, VideoService.class);
            intent.putExtra(KEY_RECEIVED_CALL, receivedCall);
            intent.putExtra(KEY_ROOM_ID, roomId);
            intent.putExtra(KEY_TEAM_ID, teamId);
            intent.putExtra(KEY_ACCOUNTS, accounts);
            intent.putExtra(KEY_TNAME, teamName);
            intent.putExtra(KEY_TYPE, callType);
            context.startService(intent);

        }

    }


    public static boolean VIDEO_ON = false;

    public VideoService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    // CONST
    private static final String TAG = "TeamAVChat";
    private static final String KEY_RECEIVED_CALL = "call";
    private static final String KEY_TEAM_ID = "teamid";
    private static final String KEY_ROOM_ID = "roomid";
    private static final String KEY_ACCOUNTS = "accounts";
    private static final String KEY_TNAME = "teamName";
    private static final String KEY_TYPE = "avchattype";
    private static final int AUTO_REJECT_CALL_TIMEOUT = 45 * 1000;
    //    private static final int CHECK_RECEIVED_CALL_TIMEOUT = 200 * 1000;
    private static final int CHECK_RECEIVED_CALL_TIMEOUT = 30 * 1000;
    private static int MAX_SUPPORT_ROOM_USERS_COUNT = 9;
    private static final int BASIC_PERMISSION_REQUEST_CODE = 0x100;

    // DATA
    private String teamId;
    private String roomId;
    private long chatId;
    private ArrayList<String> accounts;
    private boolean receivedCall;
    private boolean destroyRTC;
    private String teamName;
    private int avChatType;


    private void onIntent(Intent i) {
        Intent intent = i;
        receivedCall = intent.getBooleanExtra(KEY_RECEIVED_CALL, false);
        roomId = intent.getStringExtra(KEY_ROOM_ID);
        teamId = intent.getStringExtra(KEY_TEAM_ID);
        accounts = (ArrayList<String>) intent.getSerializableExtra(KEY_ACCOUNTS);
        teamName = intent.getStringExtra(KEY_TNAME);
        teamName = intent.getStringExtra(KEY_TNAME);
        avChatType = intent.getIntExtra(KEY_TYPE, 1);
        Log.i(TAG, "onIntent, roomId=" + roomId + ", teamId=" + teamId
                + ", receivedCall=" + receivedCall + ", accounts=" + accounts.size() + ", teamName = " + teamName + ", avChatType = " + avChatType);
    }


    private TeamAVChatNotification notifier;

    private void initNotification() {
        notifier = new TeamAVChatNotification(this);
        notifier.init(teamId, teamName);
    }

    private void onInit() {
        mainHandler = new Handler(this.getMainLooper());
    }


    // CONTEXT
    private Handler mainHandler;


    // LAYOUT
    private View callLayout;
    private View surfaceLayout;


    // VIEW
    private RecyclerView recyclerView;
    private TeamAVChatAdapter adapter;
    private List<TeamAVChatItem> data;
    private View voiceMuteButton;

    private HorizontalScrollView largeVideoHolder;
    private LinearLayout largeVideoHolder2;
    //    com.netease.nimlib.sdk.avchat.model.AVChatTextureViewRenderer largeVideo;
    com.netease.nimlib.sdk.avchat.video.AVChatSurfaceViewRenderer largeVideo;

    private boolean largeVideoOn = false;

    private void resetLargeVideo() {
        if (largeVideoHolder == null || largeVideo == null) return;
        if (largeVideoOn) {
            largeVideoHolder.setVisibility(View.VISIBLE);
            largeVideoHolder2.setVisibility(View.VISIBLE);
            largeVideo.setVisibility(View.VISIBLE);
            if (minify != null) minify.setVisibility(View.INVISIBLE);
        } else {
            largeVideoHolder.setVisibility(View.GONE);
            largeVideoHolder2.setVisibility(View.GONE);
            largeVideo.setVisibility(View.GONE);
            if (minify != null) minify.setVisibility(View.VISIBLE);
        }
    }

    @Subscribe
    public void onVideo(VideoLargeEvent event) {
        if (getItemIndex(event.getData().account) == 0) {
            return;
        }
        largeVideoOn = true;
        resetLargeVideo();
        largeItem = event.getData();
        resizeLargeVideo();
    }


    private void resizeLargeVideo() {
        if (!largeVideoOn) return;
        SimpleAVChatStateObserver.AvChatUIInfo info = stateObserver.getAvChatUIInfo(largeItem.account);
        int frameWidth = info.getWidth();
        int frameHeight = info.getHeight();
        int largeVideoHeight = SimpleAVChatStateObserver.height;
        int largeVideoWidth = (int) (largeVideoHeight / (frameHeight + 0f) * frameWidth);
        LinearLayout.LayoutParams largeVideoLayoutParams = (LinearLayout.LayoutParams) largeVideo.getLayoutParams();
        if (info.getRotate() == 0 || info.getRotate() == 180) {
            largeVideoLayoutParams.width = largeVideoWidth;
            largeVideoLayoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT;
            largeVideo.setLayoutParams(largeVideoLayoutParams);
        } else {
            largeVideoLayoutParams.width = SimpleAVChatStateObserver.width;
            largeVideoLayoutParams.height = SimpleAVChatStateObserver.height;
            largeVideo.setLayoutParams(largeVideoLayoutParams);
        }
        AVChatManager.getInstance().setupRemoteVideoRender(largeItem.account, largeVideo, false, AVChatVideoScalingType.SCALE_ASPECT_BALANCED);
    }

    TeamAVChatItem largeItem;

    private void findLayouts(View root) {
        callLayout = root.findViewById(R.id.team_avchat_call_layout);
        surfaceLayout = root.findViewById(R.id.team_avchat_surface_layout);
        voiceMuteButton = root.findViewById(R.id.avchat_shield_user);
    }


    // TIMER
    private Timer timer;
    private int seconds;
    private TextView timerText;
    private Runnable autoRejectTask;

    // CONTROL STATE
    boolean videoMute = false;
    boolean microphoneMute = false;
    boolean speakerMode = true;
    boolean speakerAndvideo = false;

    // AVCAHT OBSERVER
    private SimpleAVChatStateObserver stateObserver;
    private Observer<AVChatControlEvent> notificationObserver;
    private AVChatCameraCapturer mVideoCapturer;
    private static boolean needFinish = true;


    /**
     * ************************************ 主流程 ***************************************
     */

    private void showViews() {
        if (receivedCall) {
            showReceivedCallLayout();
        } else {
            showSurfaceLayout();
        }
    }


    /*
     * 接听界面
     */
    private void showReceivedCallLayout() {
        callLayout.setVisibility(View.VISIBLE);
        // 提示
        TextView textView = (TextView) callLayout.findViewById(R.id.received_call_tip);

        textView.setText(teamName + " 的视频通话");

        // 播放铃声
        AVChatSoundPlayer.instance().play(AVChatSoundPlayer.RingerTypeEnum.RING);

        // 拒绝
        callLayout.findViewById(R.id.refuse).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AVChatSoundPlayer.instance().stop();
                cancelAutoRejectTask();
                stopSelf();
            }
        });

        // 接听
        callLayout.findViewById(R.id.receive).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AVChatSoundPlayer.instance().stop();
                cancelAutoRejectTask();
                callLayout.setVisibility(View.GONE);
                showSurfaceLayout();
            }
        });

        startAutoRejectTask();
    }


    /**
     * ************************************ 定时任务 ***************************************
     */

    private void startTimer() {
        timer = new Timer();
        timer.schedule(timerTask, 0, 1000);
        timerText.setText("通话时长：00:00");
        talkingTimerText.setText("00:00");
    }

    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            seconds++;
            int m = seconds / 60;
            int s = seconds % 60;
            final String time = String.format(Locale.CHINA, "%02d:%02d", m, s);
            rootView.post(new Runnable() {
                @Override
                public void run() {
                    timerText.setText("通话时长：" + time);
                    talkingTimerText.setText("" + time);
                }
            });
        }
    };

    private void startTimerForCheckReceivedCall() {
        mainHandler.postDelayed(new Runnable() {//延迟45秒
            @Override
            public void run() {
                int index = 0;
                for (TeamAVChatItem item : data) {
                    if (item.type == TYPE_DATA && item.state == TeamAVChatItem.STATE.STATE_WAITING) {
                        item.state = TeamAVChatItem.STATE.STATE_END;
                        adapter.notifyItemChanged(index);
                    }
                    index++;
                }
                checkAllHangUp();
            }
        }, CHECK_RECEIVED_CALL_TIMEOUT);
    }

    private void startAutoRejectTask() {
        if (autoRejectTask == null) {
            autoRejectTask = new Runnable() {
                @Override
                public void run() {
                    AVChatSoundPlayer.instance().stop();
                    stopSelf();
                }
            };
        }

        mainHandler.postDelayed(autoRejectTask, AUTO_REJECT_CALL_TIMEOUT);
    }

    private void cancelAutoRejectTask() {
        if (autoRejectTask != null) {
            mainHandler.removeCallbacks(autoRejectTask);
        }
    }

    /*
     * 除了所有人都没接通，其他情况不做自动挂断
     */
    private void checkAllHangUp() {
        for (TeamAVChatItem item : data) {
            if (item.account != null &&
                    !item.account.equals(AVChatKit.getAccount()) &&
                    item.state != TeamAVChatItem.STATE.STATE_END) {
                return;
            }
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hangup();
                EventBus.getDefault().post(new NoPeopleJoinEvent(roomId));
                stopSelf();
            }
        }, 200);
    }

    /**
     * 通知栏
     */
    private void activeCallingNotifier(boolean active) {
        if (notifier != null) {
            if (destroyRTC) {
                notifier.activeCallingNotification(false);
            } else {
                notifier.activeCallingNotification(active);
            }
        }
    }


    /*
     * 通话界面
     */
    private void showSurfaceLayout() {
        // 列表
        surfaceLayout.setVisibility(View.VISIBLE);
        recyclerView = (RecyclerView) surfaceLayout.findViewById(R.id.recycler_view);
        largeVideo = surfaceLayout.findViewById(R.id.surface_large);
        largeVideoHolder = surfaceLayout.findViewById(R.id.surface_large_holder);
        largeVideoHolder2 = surfaceLayout.findViewById(R.id.surface_large_holder2);


        largeVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                largeVideoOn = false;
                resetLargeVideo();
                try {
                    int index = getItemIndex(largeItem.account);
                    if (index >= 0) {
                        TeamAVChatItem item = data.get(index);
                        IVideoRender surfaceView = adapter.getViewHolderSurfaceView(item);
                        Log.i(TAG, "old surfaceView  is null:" + (surfaceView == null));
                        if (surfaceView != null) {
                            item.state = TeamAVChatItem.STATE.STATE_PLAYING;
                            item.videoLive = true;
                            adapter.notifyItemChanged(index);
                            AVChatManager.getInstance().setupRemoteVideoRender(largeItem.account, surfaceView, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

//        initRecyclerView();
        initRv();

        // 通话计时
        timerText = (TextView) surfaceLayout.findViewById(R.id.timer_text);

        // 控制按钮
//        ViewGroup settingLayout = (ViewGroup) surfaceLayout.findViewById(R.id.avchat_setting_layout);
//        for (int i = 0; i < settingLayout.getChildCount(); i++) {
//            View v = settingLayout.getChildAt(i);
//            if (v instanceof RelativeLayout) {
//                ViewGroup vp = (ViewGroup) v;
//                if (vp.getChildCount() == 1) {
//                    vp.getChildAt(0).setOnClickListener(settingBtnClickListener);
//                }
//            }
//        }
        surfaceLayout.findViewById(R.id.hangup).setOnClickListener(new View.OnClickListener() {//挂断
            @Override
            public void onClick(View v) {
                hangup();
                stopSelf();
            }
        });
        surfaceLayout.findViewById(R.id.avchat_switch_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVideoCapturer.switchCamera();
            }
        });
        surfaceLayout.findViewById(R.id.avchat_enable_audio).setOnClickListener(new View.OnClickListener() {//麦克风关闭
            @Override
            public void onClick(View v) {
                AVChatManager.getInstance().muteLocalAudio(microphoneMute = !microphoneMute);
            }
        });
        //如果是 语音通话 禁止切换  摄像头禁止切换
        if (avChatType == AVChatType.AUDIO.getValue()) {
            surfaceLayout.findViewById(R.id.avchat_volume).setEnabled(false);
            surfaceLayout.findViewById(R.id.avchat_switch_camera).setEnabled(false);
        }
        surfaceLayout.findViewById(R.id.avchat_volume).setOnClickListener(new View.OnClickListener() {//点击麦克风  //视频
            @Override
            public void onClick(View v) {

                if (speakerAndvideo) {//// 视频
//                    AVChatManager.getInstance().muteLocalVideo(videoMute = !videoMute);
                    AVChatManager.getInstance().muteLocalVideo(false);
                    // 发送控制指令
//                    byte command = videoMute ? AVChatControlCommand.NOTIFY_VIDEO_OFF : AVChatControlCommand.NOTIFY_VIDEO_ON;
                    byte command = AVChatControlCommand.NOTIFY_VIDEO_ON;
                    AVChatManager.getInstance().sendControlCommand(chatId, command, null);
//                    v.setBackgroundResource(videoMute ? R.drawable.t_avchat_camera_mute_selector : R.drawable.t_avchat_camera_selector);
                    updateSelfItemVideoState(true);

                } else {//打开麦克风

                    AVChatManager.getInstance().muteLocalVideo(true);
                    AVChatManager.getInstance().setSpeaker(true);
                    byte command = AVChatControlCommand.NOTIFY_VIDEO_OFF;
                    AVChatManager.getInstance().sendControlCommand(chatId, command, null);
                    updateSelfItemVideoState(false);

                }
                speakerAndvideo = !speakerAndvideo;
            }
        });
        // 音视频权限检查
        checkPermission();
    }


    /**
     * ************************************ 音视频状态 ***************************************
     */

    private void onVideoLive(String account) {
        if (account.equals(AVChatKit.getAccount())) {
            return;
        }

        notifyVideoLiveChanged(account, true);
    }

    private void onVideoLiveEnd(String account) {
        if (account.equals(AVChatKit.getAccount())) {
            return;
        }

        notifyVideoLiveChanged(account, false);
    }

    private void notifyVideoLiveChanged(String account, boolean live) {
        int index = getItemIndex(account);
        if (index >= 0) {
            TeamAVChatItem item = data.get(index);
            item.videoLive = live;
            adapter.notifyItemChanged(index);
        }
    }

    private void onAudioVolume(Map<String, Integer> speakers) {
        for (TeamAVChatItem item : data) {
            if (speakers.containsKey(item.account)) {
                item.volume = speakers.get(item.account);
                adapter.updateVolumeBar(item);
            }
        }
    }

    private void updateSelfItemVideoState(boolean live) {
        int index = getItemIndex(AVChatKit.getAccount());
        if (index >= 0) {
            TeamAVChatItem item = data.get(index);
            item.videoLive = live;
            adapter.notifyItemChanged(index);
        }
    }


    /**
     * 挂断
     */
    private void hangup() {
        if (destroyRTC) {
            return;
        }
        try {
            AVChatManager.getInstance().stopVideoPreview();
            AVChatManager.getInstance().disableVideo();
            AVChatManager.getInstance().leaveRoom2(roomId, null);
            AVChatManager.getInstance().disableRtc();
            AVChatKit.hangUpShadowCall();  // 2020-05-22  关闭点对点
        } catch (Exception e) {
            e.printStackTrace();
        }
        destroyRTC = true;
        LogUtil.i(TAG, "destroy rtc & leave room, roomId=" + roomId);
    }


    /**
     * ************************************ 数据源 ***************************************
     */

    private void initRecyclerView() {
        // 确认数据源,自己放在首位
        data = new ArrayList<>(accounts.size() + 1);
        for (String account : accounts) {
            if (account.equals(AVChatKit.getAccount())) {
                continue;
            }

            data.add(new TeamAVChatItem(TYPE_DATA, teamId, account));
        }
        Log.i(TAG, "INIT RV1:" + data.toString());

        TeamAVChatItem selfItem = new TeamAVChatItem(TYPE_DATA, teamId, AVChatKit.getAccount());
        selfItem.state = TeamAVChatItem.STATE.STATE_PLAYING; // 自己直接采集摄像头画面
        data.add(0, selfItem);

        // 补充占位符
        int holderLength = MAX_SUPPORT_ROOM_USERS_COUNT - data.size();
        for (int i = 0; i < holderLength; i++) {
            data.add(new TeamAVChatItem(teamId));
        }
        Log.i(TAG, "INIT RV2:" + data.toString());
        // RecyclerView
        adapter = new TeamAVChatAdapter(recyclerView, data);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.addItemDecoration(new SpacingDecoration(ScreenUtil.dip2px(1), ScreenUtil.dip2px(1), true));
    }


    private void initRv() {
        data = new ArrayList<>();
        TeamAVChatItem selfItem = new TeamAVChatItem(TYPE_DATA, teamId, AVChatKit.getAccount());
        selfItem.state = TeamAVChatItem.STATE.STATE_PLAYING; // 自己直接采集摄像头画面
        data.add(0, selfItem);

        adapter = new TeamAVChatAdapter(recyclerView, data);
        Log.i(TAG, "INIT RV3:" + data.toString());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 1));
        recyclerView.addItemDecoration(new SpacingDecoration(ScreenUtil.dip2px(1), ScreenUtil.dip2px(1), true));

    }

    private int getSpanCount(int size) {
        if (size == 1) return 1;
        if (size >= 2 && size <= 4) return 2;
        else return 3;
    }

    private void onUserChange(final String account, boolean add) {
        Log.i(TAG, "onUserChange   account:" + account + "       add:" + add);

        if (add) {
            int index = getItemIndex(account);
            if (index >= 0) {
                TeamAVChatItem item = data.get(index);
                IVideoRender surfaceView = adapter.getViewHolderSurfaceView(item);
                Log.i(TAG, "old surfaceView  is null:" + (surfaceView == null));
                if (surfaceView != null) {
                    item.state = TeamAVChatItem.STATE.STATE_PLAYING;
                    item.videoLive = true;
                    adapter.notifyItemChanged(index);
                    AVChatManager.getInstance().setupRemoteVideoRender(account, surfaceView, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
                }
            } else {

                int oldSize = data.size();
                final TeamAVChatItem newItem = new TeamAVChatItem(TYPE_DATA, teamId, account);
                newItem.state = TeamAVChatItem.STATE.STATE_PLAYING;
                newItem.videoLive = true;
                data.add(newItem);

                int oldeSpan = getSpanCount(oldSize);
                int newSpan = getSpanCount(oldSize + 1);
                if (newSpan > oldeSpan) {
                    recyclerView.setLayoutManager(new GridLayoutManager(this, newSpan));
                }

                adapter.notifyItemInserted(oldSize);

                rootView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        IVideoRender surfaceView = adapter.getViewHolderSurfaceView(newItem);
                        Log.i(TAG, "new surfaceView  is null:" + (surfaceView == null));
                        Log.i("jjj", "new surfaceView  is null:" + (surfaceView == null) + "  thread:" + Thread.currentThread());
                        AVChatManager.getInstance().setupRemoteVideoRender(account, surfaceView, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
                    }
                }, 300);
            }

            updateAudioMuteButtonState();
        }


    }


    private int getItemIndex(final String account) {
        if (account == null) return -1;
        int index = 0;
        boolean find = false;
        for (TeamAVChatItem i : data) {
            if (i.account == null) {
                index++;
                continue;
            }
            if (i.account.equals(account)) {
                find = true;
                break;
            }
            index++;
        }

        return find ? index : -1;
    }


    /**
     * ************************************ 权限检查 ***************************************
     */

    private void checkPermission() {
        List<String> lackPermissions = AVChatManager.getInstance().checkPermission(VideoService.this);
        if (lackPermissions.isEmpty()) {
            onBasicPermissionSuccess();
        } else {
            String[] permissions = new String[lackPermissions.size()];
            for (int i = 0; i < lackPermissions.size(); i++) {
                permissions[i] = lackPermissions.get(i);
            }


        }
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        onPermissionChecked();
    }


    private void onPermissionChecked() {
        startRtc(); // 启动音视频
    }


    /**
     * ************************************ 音视频事件 ***************************************
     */

    private void startRtc() {
        // rtc init
        AVChatManager.getInstance().enableRtc();
        if (avChatType == AVChatType.AUDIO.getValue()) {// 音视频
            AVChatManager.getInstance().disableVideo();
        } else {
            AVChatManager.getInstance().enableVideo();
        }
        LogUtil.i(TAG, "start rtc done");

        mVideoCapturer = AVChatVideoCapturerFactory.createCameraCapturer(true);
        AVChatManager.getInstance().setupVideoCapturer((com.netease.nimlib.sdk.avchat.video.AVChatVideoCapturer) mVideoCapturer);

        // state observer
        if (stateObserver != null) {
            AVChatManager.getInstance().observeAVChatState(stateObserver, false);
        }


        stateObserver = new SimpleAVChatStateObserver() {

            @Override
            public void onVideoFrameResolutionChanged(String user, int width, int height, int rotate) {
                super.onVideoFrameResolutionChanged(user, width, height, rotate);
                Log.i("kkk", "onVideoFrameResolutionChanged:   user:" + user + "    width:" + width + "    height:" + height + "   rotate:" + rotate);
                resizeLargeVideo();
            }

            @Override
            public boolean onVideoFrameFilter(AVChatVideoFrame frame, boolean maybeDualInput) {
                Log.i("kkk", "onVideoFrameFilter: " + frame.width + " " + frame.height);
                return super.onVideoFrameFilter(frame, maybeDualInput);
            }

            @Override
            public void onFirstVideoFrameRendered(String user) {
                Log.i("kkk", "onFirstVideoFrameRendered:" + user);
            }

            @Override
            public void onFirstVideoFrameAvailable(String account) {
                Log.i("kkk", "onFirstVideoFrameAvailable:" + account);
            }

            @Override
            public void onJoinedChannel(int code, String audioFile, String videoFile, int i) {
                if (code == 200) {
                    onJoinRoomSuccess();
                } else {
                    onJoinRoomFailed(code, null);
                }
            }

            @Override
            public void onUserJoined(String account) {
//                onAVChatUserJoined(account);
                Log.i("kkk", "onUserJoined:" + account);
                onUserChange(account, true);
            }

            @Override
            public void onUserLeave(String account, int event) {
                onAVChatUserLeave(account);
            }

            @Override
            public void onReportSpeaker(Map<String, Integer> speakers, int mixedEnergy) {
                onAudioVolume(speakers);
            }
        };
        AVChatManager.getInstance().observeAVChatState(stateObserver, true);
        LogUtil.i(TAG, "observe rtc state done");

        // notification observer
        if (notificationObserver != null) {
            AVChatManager.getInstance().observeControlNotification(notificationObserver, false);
        }
        notificationObserver = new Observer<AVChatControlEvent>() {

            @Override
            public void onEvent(AVChatControlEvent event) {
                final String account = event.getAccount();
                if (AVChatControlCommand.NOTIFY_VIDEO_ON == event.getControlCommand()) {
                    onVideoLive(account);
                } else if (AVChatControlCommand.NOTIFY_VIDEO_OFF == event.getControlCommand()) {
                    onVideoLiveEnd(account);
                }
            }
        };
        AVChatManager.getInstance().observeControlNotification(notificationObserver, true);

        // join
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_SESSION_MULTI_MODE_USER_ROLE, AVChatUserRole.NORMAL);
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_AUDIO_REPORT_SPEAKER, true);
        AVChatManager.getInstance().setParameter(AVChatParameters.KEY_VIDEO_FIXED_CROP_RATIO, AVChatVideoCropRatio.CROP_RATIO_1_1);
        AVChatManager.getInstance().joinRoom2(roomId, AVChatType.VIDEO, new AVChatCallback<AVChatData>() {
            @Override
            public void onSuccess(AVChatData data) {
                chatId = data.getChatId();
                LogUtil.i(TAG, "join room success, roomId=" + roomId + ", chatId=" + chatId);
            }

            @Override
            public void onFailed(int code) {
                onJoinRoomFailed(code, null);
                LogUtil.i(TAG, "join room failed, code=" + code + ", roomId=" + roomId);
            }

            @Override
            public void onException(Throwable exception) {
                onJoinRoomFailed(-1, exception);
                LogUtil.i(TAG, "join room failed, e=" + exception.getMessage() + ", roomId=" + roomId);
            }
        });
        LogUtil.i(TAG, "start join room, roomId=" + roomId);
    }

    private void onJoinRoomSuccess() {
        startTimer();
        startLocalPreview();
        startTimerForCheckReceivedCall();
        LogUtil.i(TAG, "team avchat running..." + ", roomId=" + roomId);
    }

    private void onJoinRoomFailed(int code, Throwable e) {
        if (code == ResponseCode.RES_ENONEXIST) {
            Toast.makeText(this, getString(R.string.t_avchat_join_fail_not_exist), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "join room failed, code=" + code + ", e=" + (e == null ? "" : e.getMessage()), Toast.LENGTH_SHORT).show();
        }
    }

    public void onAVChatUserJoined(String account) {
        int index = getItemIndex(account);
        if (index >= 0) {
            TeamAVChatItem item = data.get(index);
            IVideoRender surfaceView = adapter.getViewHolderSurfaceView(item);
            if (surfaceView != null) {
                item.state = TeamAVChatItem.STATE.STATE_PLAYING;
                item.videoLive = true;
                adapter.notifyItemChanged(index);
                AVChatManager.getInstance().setupRemoteVideoRender(account, surfaceView, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
            }
        }
        updateAudioMuteButtonState();
        LogUtil.i(TAG, "on user joined, account=" + account + "  index:" + index);
    }

    public void onAVChatUserLeave(String account) {
        int index = getItemIndex(account);
        if (index >= 0) {
            TeamAVChatItem item = data.get(index);
            item.state = TeamAVChatItem.STATE.STATE_HANGUP;
            item.volume = 0;
            adapter.notifyItemChanged(index);
        }
        updateAudioMuteButtonState();

        LogUtil.i(TAG, "on user leave, account=" + account);
    }

    private void startLocalPreview() {
        LogUtil.i(TAG, "startLocalPreview=");
        if (data.size() >= 1 && data.get(0).account != null && data.get(0).account.equals(AVChatKit.getAccount())) {
            IVideoRender surfaceView = adapter.getViewHolderSurfaceView(data.get(0));
            if (surfaceView != null) {
                AVChatManager.getInstance().setupLocalVideoRender(surfaceView, false, AVChatVideoScalingType.SCALE_ASPECT_FIT);
                AVChatManager.getInstance().startVideoPreview();
                data.get(0).state = TeamAVChatItem.STATE.STATE_PLAYING;
                data.get(0).videoLive = true;
                adapter.notifyItemChanged(0);
                LogUtil.i(TAG, "startLocalPreview end");
            }
        }
    }


    /**
     * ************************************ 点击事件 ***************************************
     */

    private View.OnClickListener settingBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int i = v.getId();
            if (i == R.id.avchat_switch_camera) {// 切换前后摄像头
                mVideoCapturer.switchCamera();

            } else if (i == R.id.avchat_enable_video) {// 视频
                AVChatManager.getInstance().muteLocalVideo(videoMute = !videoMute);
                // 发送控制指令
                byte command = videoMute ? AVChatControlCommand.NOTIFY_VIDEO_OFF : AVChatControlCommand.NOTIFY_VIDEO_ON;
                AVChatManager.getInstance().sendControlCommand(chatId, command, null);
                v.setBackgroundResource(videoMute ? R.drawable.t_avchat_camera_mute_selector : R.drawable.t_avchat_camera_selector);
                updateSelfItemVideoState(!videoMute);

            } else if (i == R.id.avchat_enable_audio) {// 麦克风开关
                Toast.makeText(VideoService.this, "麦克风开关", Toast.LENGTH_SHORT).show();
                AVChatManager.getInstance().muteLocalAudio(microphoneMute = !microphoneMute);
//               v.setBackgroundResource(microphoneMute ? R.drawable.ic_microphone_pressed : R.drawable.ic_microphone);

            } else if (i == R.id.avchat_volume) {// 听筒扬声器切换
                AVChatManager.getInstance().setSpeaker(speakerMode = !speakerMode);
                v.setBackgroundResource(speakerMode ? R.drawable.t_avchat_speaker_selector : R.drawable.t_avchat_speaker_mute_selector);

            } else if (i == R.id.avchat_shield_user) {// 屏蔽用户音频
                disableUserAudio();

            } else if (i == R.id.hangup) {// 挂断
                hangup();
                stopSelf();

            }
        }
    };

    private void updateAudioMuteButtonState() {
        boolean enable = false;
        for (TeamAVChatItem item : data) {
            if (item.state == TeamAVChatItem.STATE.STATE_PLAYING &&
                    !AVChatKit.getAccount().equals(item.account)) {
                enable = true;
                break;
            }
        }
        voiceMuteButton.setEnabled(enable);
        voiceMuteButton.invalidate();
    }

    private void disableUserAudio() {
        List<Pair<String, Boolean>> voiceMutes = new ArrayList<>();
        for (TeamAVChatItem item : data) {
            if (item.state == TeamAVChatItem.STATE.STATE_PLAYING &&
                    !AVChatKit.getAccount().equals(item.account)) {
                voiceMutes.add(new Pair<>(item.account, AVChatManager.getInstance().isRemoteAudioMuted(item.account)));
            }
        }
        TeamAVChatVoiceMuteDialog dialog = new TeamAVChatVoiceMuteDialog(this, teamId, voiceMutes);
        dialog.setTeamVoiceMuteListener(new TeamAVChatVoiceMuteDialog.TeamVoiceMuteListener() {
            @Override
            public void onVoiceMuteChange(List<Pair<String, Boolean>> voiceMuteAccounts) {
                if (voiceMuteAccounts != null) {
                    for (Pair<String, Boolean> voiceMuteAccount : voiceMuteAccounts) {
                        AVChatManager.getInstance().muteRemoteAudio(voiceMuteAccount.first, voiceMuteAccount.second);
                    }
                }
            }
        });
        dialog.show();
    }


    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "---onCreate----");
        EventBus.getDefault().register(this);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        layoutParams = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.x = 0;
        layoutParams.y = 0;


    }

    private RelativeLayout displayView;
    private CostumFrame rootView;
    View minify;
    ViewState state;
    View talkingIcon;
    TextView talkingTimerText;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onInit();
        onIntent(intent);
        ViewState.ViewStateSmall.WIDTH = (int) getResources().getDimension(R.dimen.talking_window_small_width);
        ViewState.ViewStateSmall.HEIGHT = (int) getResources().getDimension(R.dimen.talking_window_small_height);
        initNotification();


        showFloatingWindow();
        showViews();

        return super.onStartCommand(intent, flags, startId);
    }


    static Callable minifyCall;

    public static boolean minify() {
        if (minifyCall == null) return false;
        else {
            boolean minified = false;
            try {
                minified = (boolean) minifyCall.call();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return minified;
        }
    }

    private void showFloatingWindow() {

        VIDEO_ON = true;
        Log.i(TAG, "---showFloatingWindow----");
        if (PermissionUtil.hasOverlayPermission(this)) {
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            rootView = (CostumFrame) layoutInflater.inflate(R.layout.floating_window, null);


            findLayouts(rootView);

            displayView = rootView.findViewById(R.id.vdh_holder);
            minify = displayView.findViewById(R.id.minify_call);
            talkingIcon = rootView.findViewById(R.id.talking_icon);
            talkingTimerText = rootView.findViewById(R.id.small_timer_text);

            minifyCall = new Callable() {
                @Override
                public Object call() throws Exception {

                    if (state.getCallViewState() == CallViewState.FULL) {

                        ViewState newState = new ViewState.ViewStateSmall(layoutParams);
                        applyViewState(newState);
                        return true;
                    } else if (state.getCallViewState() == CallViewState.SMALL) {

                        return false;
                    }
                    return false;
                }
            };

            minify.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (state.getCallViewState() == CallViewState.FULL) {
                        ViewState newState = new ViewState.ViewStateSmall(layoutParams);
                        applyViewState(newState);
                    } else if (state.getCallViewState() == CallViewState.SMALL) {
                        ViewState newState = new ViewState.ViewStateFull(layoutParams);
                        applyViewState(newState);
                    }
                }
            });

            windowManager.addView(rootView, layoutParams);

            ViewState initState = new ViewState.ViewStateFull(layoutParams);
            applyViewState(initState);

        }
    }


    void repositionViewOnTouch(ViewState newState) {
        state = newState;
        layoutParams = state.getRootLayout();
        windowManager.updateViewLayout(rootView, layoutParams);
    }


    void applyViewState(ViewState newState) {
        state = newState;
        layoutParams = state.getRootLayout();
        windowManager.updateViewLayout(rootView, layoutParams);

        if (state.getCallViewState() == CallViewState.SMALL) {

            Log.i(TAG, "---setOnTouchListener----");

            rootView.setInterceptOn(true);
            rootView.setOnTouchListener(new View.OnTouchListener() {

                float downX, downY;
                float preY, preX;
                float x, y;
                long downTime;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    int action = event.getAction();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            preY = event.getRawY();
                            preX = event.getRawX();
                            downTime = System.currentTimeMillis();
                            downX = preX;
                            downY = preY;

                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_MOVE:
                            y = event.getRawY();
                            x = event.getRawX();
                            float deltaY = y - preY;
                            float deltaX = x - preX;

                            layoutParams.x = (layoutParams.x + Math.round(deltaX));
                            layoutParams.y = (layoutParams.y + Math.round(deltaY));
                            repositionViewOnTouch(new ViewState.ViewStateSmall(layoutParams));
                            preY = y;
                            preX = x;


                            if (action == MotionEvent.ACTION_UP) {
                                long touchLength = System.currentTimeMillis() - downTime;
                                float delta2 = (y - downY) * (y - downY) + (x - downX) * (x - downX);
                                if (touchLength < 300 && delta2 < 25) {
                                    applyViewState(new ViewState.ViewStateFull(layoutParams));
                                }
                            }
                            break;

                    }
                    return true;
                }
            });

            talkingIcon.setVisibility(View.VISIBLE);
            talkingTimerText.setVisibility(View.VISIBLE);
            minify.setVisibility(View.GONE);
            resetLargeVideo();

        } else {

            Log.i(TAG, "---setOnTouchListener  null----");
            rootView.setInterceptOn(false);
            rootView.setOnTouchListener(null);

            talkingIcon.setVisibility(View.GONE);
            talkingTimerText.setVisibility(View.GONE);
            minify.setVisibility(View.VISIBLE);
            resetLargeVideo();
            if (adapter != null)
                adapter.notifyDataSetChanged();

        }

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        VIDEO_ON = false;
        minifyCall = null;

        windowManager.removeView(rootView);

        needFinish = true;
        if (timer != null) {
            timer.cancel();
        }

        if (stateObserver != null) {
            AVChatManager.getInstance().observeAVChatState(stateObserver, false);
        }

        if (notificationObserver != null) {
            AVChatManager.getInstance().observeControlNotification(notificationObserver, false);
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        hangup(); // 页面销毁的时候要保证离开房间，rtc释放。
        activeCallingNotifier(false);
        setChatting(false);
        NIMClient.getService(AuthServiceObserver.class).observeOnlineStatus(userStatusObserver, false);

    }


    /*
     * 设置通话状态
     */
    private void setChatting(boolean isChatting) {
        TeamAVChatProfile.sharedInstance().setTeamAVChatting(isChatting);
    }


    /**
     * 在线状态观察者
     */
    private Observer<StatusCode> userStatusObserver = new Observer<StatusCode>() {

        @Override
        public void onEvent(StatusCode code) {

            if (code.getValue() == 7) {
                NIMClient.getService(AuthService.class).getKickedClientType();
            }


            if (code.wontAutoLogin()) {
                AVChatSoundPlayer.instance().stop();
                hangup();
                stopSelf();
            }
        }
    };


}
