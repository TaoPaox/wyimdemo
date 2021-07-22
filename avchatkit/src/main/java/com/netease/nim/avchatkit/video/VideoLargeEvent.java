package com.netease.nim.avchatkit.video;

import com.netease.nim.avchatkit.teamavchat.module.TeamAVChatItem;

/**
 * Created by nanyi on 2019/11/1.
 */

public class VideoLargeEvent  {
    TeamAVChatItem data;

    public TeamAVChatItem getData() {
        return data;
    }

    public VideoLargeEvent(TeamAVChatItem data) {
        this.data = data;
    }
}
