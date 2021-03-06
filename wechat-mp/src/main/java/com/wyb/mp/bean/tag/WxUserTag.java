package com.wyb.mp.bean.tag;

import java.io.Serializable;
import java.util.List;

import lombok.Data;

import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.wyb.mp.util.json.WxMpGsonBuilder;

/**
 * <pre>
 * 用户标签对象
 * </pre>
 *
 */
@Data
public class WxUserTag implements Serializable {
    private static final long serialVersionUID = -7722428695667031252L;

    /**
     * 标签id，由微信分配.
     */
    private Long id;

    /**
     * 标签名，UTF8编码.
     */
    private String name;

    /**
     * 此标签下粉丝数.
     */
    private Integer count;

    public static WxUserTag fromJson(String json) {
        return WxMpGsonBuilder.create().fromJson(new JsonParser().parse(json).getAsJsonObject().get("tag"),
                WxUserTag.class);
    }

    public static List<WxUserTag> listFromJson(String json) {
        return WxMpGsonBuilder.create().fromJson(new JsonParser().parse(json).getAsJsonObject().get("tags"),
                new TypeToken<List<WxUserTag>>() {
                }.getType());
    }

    public String toJson() {
        return WxMpGsonBuilder.create().toJson(this);
    }

    @Override
    public String toString() {
        return this.toJson();
    }
}
