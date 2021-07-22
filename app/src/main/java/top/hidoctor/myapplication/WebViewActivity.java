package top.hidoctor.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;

import com.github.lzyzsd.jsbridge.BridgeHandler;
import com.github.lzyzsd.jsbridge.BridgeWebView;
import com.github.lzyzsd.jsbridge.CallBackFunction;


public class WebViewActivity extends AppCompatActivity {
    private String url="https://t1.hidoctor.wiki/mobile/consult/list ";
    private BridgeWebView mWebview;
    private String TAG="WebViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);
        initWebview();
    }

    private void initWebview() {
        mWebview = findViewById(R.id.webview);

        WebSettings settings = mWebview.getSettings();
        String userAgentString = settings.getUserAgentString();
        settings.setUserAgentString(userAgentString+" hidoctor");



        mWebview.registerHandler("notifyNative", new BridgeHandler() {
            @Override
            public void handler(String data, CallBackFunction function) {
                Log.e(TAG, "handler = submitFromWeb, data from web = " + data);
                function.onCallBack("submitFromWeb exe, response data from Java");
            }
        });

        mWebview.loadUrl(url);
    }
}