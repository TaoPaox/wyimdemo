package com.netease.nim.avchatkit.extension;

import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachment;
import com.netease.nimlib.sdk.msg.attachment.MsgAttachmentParser;

/**
 * Created by zhoujianghua on 2015/4/9.
 */
public class CustomAttachParser implements MsgAttachmentParser {

    private static final String KEY_TYPE = "type";
    private static final String KEY_DATA = "data";

    @Override
    public MsgAttachment parse(String json) {
        Log.i("NIM9","parser   --> json: "+json);
        Log.i("NIM2","parser   --> json: "+json);
        CustomAttachment attachment = null;
        try {
            JSONObject object = JSON.parseObject(json);
//            int type = object.getInteger(KEY_TYPE);   //  ----->  only default attachemnt
            int type = 0;
            JSONObject data = object.getJSONObject(KEY_DATA);
            switch (type) {
                case CustomAttachmentType.Guess:
                    attachment = new GuessAttachment();
                    break;
                case CustomAttachmentType.SnapChat:
                    return new SnapChatAttachment(data);
                case CustomAttachmentType.Sticker:
                    attachment = new StickerAttachment();
                    break;
                case CustomAttachmentType.RTS:
                    attachment = new RTSAttachment();
                    break;
                case CustomAttachmentType.RedPacket:
                    attachment = new RedPacketAttachment();
                    break;
                case CustomAttachmentType.OpenedRedPacket:
                    attachment = new RedPacketOpenedAttachment();
                    break;
                default:
                    attachment = new DefaultCustomAttachment();
                    attachment.fromJson(object);
                    return attachment;

            }
            if (attachment != null) {
                attachment.fromJson(data);
            }
        } catch (Exception e) {

        }
        return attachment;
    }

    public static String packData(int type, JSONObject data) {
        Log.i("NIM2","parser  --> packData: ");
        if(type == 0)  {
            if(data!=null){
                return data.toString();
            }
            else return "{}";
        }
        JSONObject object = new JSONObject();
        object.put(KEY_TYPE, type);
        if (data != null) {
            object.put(KEY_DATA, data);
        }
        return object.toJSONString();
    }
}
