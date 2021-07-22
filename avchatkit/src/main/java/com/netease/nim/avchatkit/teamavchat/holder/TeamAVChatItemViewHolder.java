package com.netease.nim.avchatkit.teamavchat.holder;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.bumptech.glide.Glide;
import com.netease.nim.avchatkit.AVChatKit;
import com.netease.nim.avchatkit.R;
import com.netease.nim.avchatkit.common.recyclerview.adapter.BaseMultiItemFetchLoadAdapter;
import com.netease.nim.avchatkit.common.recyclerview.holder.BaseViewHolder;
import com.netease.nim.avchatkit.teamavchat.module.TeamAVChatItem;
import com.netease.nim.avchatkit.video.VideoLargeEvent;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.avchat.model.AVChatTextureViewRenderer;
import com.netease.nimlib.sdk.nos.model.NosThumbParam;
import com.netease.nimlib.sdk.nos.util.NosThumbImageUtil;
import com.netease.nimlib.sdk.uinfo.UserService;
import com.netease.nimlib.sdk.uinfo.model.NimUserInfo;
import com.netease.nimlib.sdk.uinfo.model.UserInfo;
import com.netease.nrtc.video.render.IVideoRender;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;

/**
 * Created by huangjun on 2017/5/4.
 */

public class TeamAVChatItemViewHolder extends TeamAVChatItemViewHolderBase {
    private static final int DEFAULT_AVATAR_THUMB_SIZE = (int) AVChatKit.getContext().getResources().getDimension(R.dimen.avatar_max_size);
    private ImageView avatarImage;
    private ImageView loadingImage;
    private AVChatTextureViewRenderer surfaceView;
    private TextView nickNameText;
    private TextView stateText;
    private ProgressBar volumeBar;

    private View holderView;


    public TeamAVChatItemViewHolder(BaseMultiItemFetchLoadAdapter adapter) {
        super(adapter);
    }

    protected void inflate(final BaseViewHolder holder) {
        avatarImage = holder.getView(R.id.avatar_image);
        loadingImage = holder.getView(R.id.loading_image);
        surfaceView = holder.getView(R.id.surface);
        nickNameText = holder.getView(R.id.nick_name_text);
        stateText = holder.getView(R.id.avchat_state_text);
        volumeBar = holder.getView(R.id.avchat_volume);
        holderView = holder.getView(R.id.holder_view);
    }

    protected void refresh(final TeamAVChatItem data) {
//        nickNameText.setText(AVChatKit.getTeamDataProvider().getDisplayNameWithoutMe(data.teamId, data.account));
        if(holderView!=null){
            holderView.setTag(R.id.holder_view,data);
            holderView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TeamAVChatItem data = (TeamAVChatItem) v.getTag(R.id.holder_view);
                    Log.i("TeamAVChat","data.account:"+data.account);
                    EventBus.getDefault().post(new VideoLargeEvent(data));
                }
            });
        }

        final UserInfo userInfo = AVChatKit.getUserInfoProvider().getUserInfo(data.account);

        List<String> accounts = new ArrayList<>();
        accounts.add(data.account);

        NIMClient.getService(UserService.class).fetchUserInfo(accounts)
                .setCallback(new RequestCallback<List<NimUserInfo>>() {
                    @Override
                    public void onSuccess(List<NimUserInfo> nimUserInfos) {
                        NimUserInfo info = nimUserInfos.get(0);
                        Log.i("TeamAVChat", "userInfo--->  getAccount:" + info.getAccount()+"   getName:"+info.getName());
                        nickNameText.setText(info.getName());
                    }

                    @Override
                    public void onFailed(int i) {
                        Log.i("TeamAVChat", "onFailed  : ");
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        Log.i("TeamAVChat", "onException  : " );
                    }
                });


//        String info = JSON.toJSONString(userInfo);
//        Log.i("TeamAVChat", "userInfo  : " + info);
//        try {
//            JSONObject obj = new JSONObject(info);
//            String name = obj.has("name") ? obj.getString("name") : obj.getString("account");
////            Log.e("TAG", "refresh: " + obj.toString());
//            nickNameText.setText(name);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }



        final int defaultResId = R.drawable.t_avchat_avatar_default;
        final String thumbUrl = makeAvatarThumbNosUrl(userInfo != null ? userInfo.getAvatar() : null, DEFAULT_AVATAR_THUMB_SIZE);
        Glide.with(AVChatKit.getContext())
                .load(thumbUrl)
                .asBitmap()
                .centerCrop()
                .placeholder(defaultResId)
                .error(defaultResId)
                .override(DEFAULT_AVATAR_THUMB_SIZE, DEFAULT_AVATAR_THUMB_SIZE)
                .into(avatarImage);
        if (data.state == TeamAVChatItem.STATE.STATE_WAITING) {
            // 等待接听
            Glide.with(AVChatKit.getContext())
                    .load(R.drawable.t_avchat_loading)
                    .asGif()
                    .into(loadingImage);
            loadingImage.setVisibility(View.VISIBLE);
            surfaceView.setVisibility(View.INVISIBLE);
            stateText.setVisibility(GONE);
        } else if (data.state == TeamAVChatItem.STATE.STATE_PLAYING) {
            // 正在通话
            loadingImage.setVisibility(GONE);
            surfaceView.setVisibility(data.videoLive ? View.VISIBLE : View.INVISIBLE); // 有视频流才需要SurfaceView
            stateText.setVisibility(GONE);
        } else if (data.state == TeamAVChatItem.STATE.STATE_END || data.state == TeamAVChatItem.STATE.STATE_HANGUP) {
            // 未接听/挂断
            loadingImage.setVisibility(GONE);
            surfaceView.setVisibility(GONE);
            stateText.setVisibility(View.VISIBLE);
            stateText.setText(data.state == TeamAVChatItem.STATE.STATE_HANGUP ? R.string.avchat_has_hangup : R.string.avchat_no_pick_up);
        }

        updateVolume(data.volume);
    }

    /**
     * 生成头像缩略图NOS URL地址（用作ImageLoader缓存的key）
     */
    private static String makeAvatarThumbNosUrl(final String url, final int thumbSize) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        return thumbSize > 0 ? NosThumbImageUtil.makeImageThumbUrl(url, NosThumbParam.ThumbType.Crop, thumbSize, thumbSize) : url;
    }

    public IVideoRender getSurfaceView() {
        return surfaceView;
    }

    public void updateVolume(int volume) {
        volumeBar.setProgress(volume);
    }
}
