package top.hidoctor.myapplication;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.StatusCode;
import com.netease.nimlib.sdk.auth.AuthService;
import com.netease.nimlib.sdk.auth.LoginInfo;


/**
 * Created by nanyi on 2020/6/15.
 * <p>
 * 云信登录状态管理
 */

public class NimLoginStateManager {


    private final Context content;

    public static void setNeedToLoginToNetease() {
        needToLoginToNetEase = true;
    }

    private static boolean needToLoginToNetEase = false;

    private static boolean needToLoginToNetEase() {
        if (needToLoginToNetEase) {
            needToLoginToNetEase = false;
            return true;
        } else {
            return false;
        }
    }

    public NimLoginStateManager(Context content) {
        this.content = content;
    }

    public void doLoginToNeteaseOnResume() {
        StatusCode status = NIMClient.getStatus();
        if (needToLoginToNetEase() || (status != StatusCode.LOGINED)) {
            LoginInfo info = loginInfo();
            RequestCallback<LoginInfo> callback =
                    new RequestCallback<LoginInfo>() {
                        @Override
                        public void onSuccess(LoginInfo loginInfo) {
                        }

                        @Override
                        public void onFailed(int i) {
                            if (i == 302) {
//                                ToastUtil.showToast(MainHtmlActivity.this,"云信token失效,请重新登陆。",Toast.LENGTH_LONG);
//                                MainHtmlActivity.this.getLogoutApi().logout();
                            }
                        }
                        @Override
                        public void onException(Throwable throwable) {
                        }
                    };
            NIMClient.getService(AuthService.class).login(info)
                    .setCallback(callback);
        } else {

        }
    }


    public LoginInfo loginInfo() {
//        String account = AccountUtil.getNimAccid(content);// todo
        String account = "";// todo
        String token = "";// todo
        String appKey = "";// todo
        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
            // 设置DemoCache中的account
            ChatCache.setAccount(account.toLowerCase());
            LoginInfo info = new LoginInfo(account.toLowerCase(), token, appKey);
            return info; // 返回LoginInfo对象
        } else {
            return null;
        }
    }


    // 如果已经存在用户登录信息，返回LoginInfo，否则返回null即可
//    public LoginInfo loginInfo() {
//        String account = "60ac6fb6cdf3bd10e2b493bb"; // todo  用户账号
//        String token = "7b60f7cbb06394eb65a6f9f0498099bb"; // todo 登录token
//
//        if (!TextUtils.isEmpty(account) && !TextUtils.isEmpty(token)) {
//            // 设置DemoCache中的account
//            ChatCache.setAccount(account.toLowerCase());
//            String appKey = "fd2e0e70a66a445c4e8695a958eae14a"; //todo  应用的AppKey（可选）
//            if (!TextUtils.isEmpty(appKey)) {
//                return new LoginInfo(account, token, appKey); // 返回LoginInfo对象
//            }
//            return new LoginInfo(account, token); // 返回LoginInfo对象
//        } else {
//            return null;
//        }
//    }

}
