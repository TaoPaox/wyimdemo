package top.hidoctor.myapplication;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nim.avchatkit.TeamAVChatProfile;
import com.netease.nim.avchatkit.activity.AVChatActivity;
import com.netease.nim.avchatkit.common.permission.MPermission;
import com.netease.nim.avchatkit.common.permission.annotation.OnMPermissionDenied;
import com.netease.nim.avchatkit.common.permission.annotation.OnMPermissionGranted;
import com.netease.nim.avchatkit.common.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.avchat.AVChatCallback;
import com.netease.nimlib.sdk.avchat.AVChatManager;
import com.netease.nimlib.sdk.avchat.constant.AVChatType;
import com.netease.nimlib.sdk.avchat.model.AVChatChannelInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPermission();
            }
        });



        findViewById(R.id.btn_yc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, WebViewActivity.class));

            }
        });


    }

    private void checkPermission() {
        List<String> lackPermissions = AVChatManager.getInstance().checkPermission(MainActivity.this);
        if (lackPermissions.isEmpty()) {
            onBasicPermissionSuccess();
        } else {
            String[] permissions = new String[lackPermissions.size()];
            for (int i = 0; i < lackPermissions.size(); i++) {
                permissions[i] = lackPermissions.get(i);
            }
            MPermission.with(MainActivity.this)
                    .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                    .permissions(permissions)
                    .request();
        }
    }


    private static final int BASIC_PERMISSION_REQUEST_CODE = 0x100;


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


    private void startVideo() {
        NimLoginStateManager nimLoginStateManager = new NimLoginStateManager(this);
        String consultId = "60d99944cdf3d8a2db66b65b";// todo 房间号

        StatusCode status = NIMClient.getStatus();
        if (status != StatusCode.LOGINED) {
            nimLoginStateManager.doLoginToNeteaseOnResume();

            NIMClient.getService(AuthService.class).login(nimLoginStateManager.loginInfo())
                    .setCallback(new RequestCallback() {
                        @Override
                        public void onSuccess(Object param) {
                            startAVChat(consultId);
                        }

                        @Override
                        public void onFailed(int code) {
                            Log.e(TAG, "onFailed: "+code );
                        }

                        @Override
                        public void onException(Throwable exception) {
                            Log.e(TAG, "onException: "+exception.getMessage() );
                        }
                    });
        } else {
            startAVChat(consultId);
        }
    }

    ArrayList<String> accounts = new ArrayList<>(); // 房间 成员

    private void startAVChat(String roomName) {
        int callType = AVChatType.VIDEO.getValue(); // 默认 视频
        //转诊 用转诊id
        AVChatManager.getInstance().createRoom(roomName, null, new AVChatCallback<AVChatChannelInfo>() {
            @Override
            public void onSuccess(AVChatChannelInfo avChatChannelInfo) {
                //发送视频通话推送

                Log.e(TAG, "onSuccess" + "创建成功");
                TeamAVChatProfile.sharedInstance().setTeamAVChatting(true);
                AVChatKit.outgoingTeamCall(MainActivity.this, false,
                        "", roomName, accounts, "", callType);
            }

            @Override
            public void onFailed(int i) {
                if (i == 417) {
                    //重新进入
                    AVChatKit.outgoingTeamCall(MainActivity.this, false,
                            "", roomName, accounts, "", callType);
                }
            }

            @Override
            public void onException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }


}