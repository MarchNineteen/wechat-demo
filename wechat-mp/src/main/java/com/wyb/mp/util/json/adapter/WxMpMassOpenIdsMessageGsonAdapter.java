package com.wyb.mp.util.json.adapter;

import java.lang.reflect.Type;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.*;
import com.wyb.common.api.WxConsts;
import com.wyb.mp.bean.message.WxMpMassOpenIdsMessage;

/**
 * @author Kunzite
 */
public class WxMpMassOpenIdsMessageGsonAdapter implements JsonSerializer<WxMpMassOpenIdsMessage> {

    @Override
    public JsonElement serialize(WxMpMassOpenIdsMessage message, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject messageJson = new JsonObject();

        JsonArray toUsers = new JsonArray();
        for (String openId : message.getToUsers()) {
            toUsers.add(new JsonPrimitive(openId));
        }
        messageJson.add("touser", toUsers);

        if (WxConsts.MassMsgType.MPNEWS.equals(message.getMsgType())) {
            JsonObject sub = new JsonObject();
            sub.addProperty("media_id", message.getMediaId());
            messageJson.add(WxConsts.MassMsgType.MPNEWS, sub);
        }
        if (WxConsts.MassMsgType.TEXT.equals(message.getMsgType())) {
            JsonObject sub = new JsonObject();
            sub.addProperty("content", message.getContent());
            messageJson.add(WxConsts.MassMsgType.TEXT, sub);
        }
        if (WxConsts.MassMsgType.VOICE.equals(message.getMsgType())) {
            JsonObject sub = new JsonObject();
            sub.addProperty("media_id", message.getMediaId());
            messageJson.add(WxConsts.MassMsgType.VOICE, sub);
        }
        if (WxConsts.MassMsgType.IMAGE.equals(message.getMsgType())) {
            JsonObject sub = new JsonObject();
            sub.addProperty("media_id", message.getMediaId());
            messageJson.add(WxConsts.MassMsgType.IMAGE, sub);
        }
        if (WxConsts.MassMsgType.MPVIDEO.equals(message.getMsgType())) {
            JsonObject sub = new JsonObject();
            sub.addProperty("media_id", message.getMediaId());
            messageJson.add(WxConsts.MassMsgType.MPVIDEO, sub);
        }
        messageJson.addProperty("msgtype", message.getMsgType());
        messageJson.addProperty("send_ignore_reprint", message.isSendIgnoreReprint() ? 0 : 1);

        if (StringUtils.isNotEmpty(message.getClientMsgId())) {
            messageJson.addProperty("clientmsgid", message.getClientMsgId());
        }

        return messageJson;
    }

}
