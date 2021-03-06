package com.wyb.mp.api;

import java.util.concurrent.locks.Lock;

import com.wyb.common.bean.WxAccessToken;
import com.wyb.mp.bean.WxMpHostConfig;
import com.wyb.mp.enums.TicketType;

/**
 * 微信配置接口
 *
 * @author Kunzite
 */
public interface WxMpConfigStorage {

    /**
     * 获取accessToken
     */
    String getAccessToken();

    /**
     * accessToken是否过期
     */
    boolean isAccessTokenExpired();

    /**
     * 强制将access token过期掉.
     */
    void expireAccessToken();

    /**
     * 应该是线程安全的.
     *
     * @param accessToken
     *            要更新的WxAccessToken对象
     */
    void updateAccessToken(WxAccessToken accessToken);

    /**
     * 应该是线程安全的.
     *
     * @param accessToken
     *            新的accessToken值
     * @param expiresInSeconds
     *            过期时间，以秒为单位
     */
    void updateAccessToken(String accessToken, int expiresInSeconds);

    String getTicket(TicketType type);

    Lock getTicketLock(TicketType type);

    boolean isTicketExpired(TicketType type);

    /**
     * 强制将ticket过期掉.
     */
    void expireTicket(TicketType type);

    /**
     * 更新ticket. 应该是线程安全的
     *
     * @param type
     *            ticket类型
     * @param ticket
     *            新的ticket值
     * @param expiresInSeconds
     *            过期时间，以秒为单位
     */
    void updateTicket(TicketType type, String ticket, int expiresInSeconds);

    String getAppId();

    String getSecret();

    String getToken();

    String getAesKey();

    long getExpiresTime();

    /**
     * 是否自动刷新token.
     */
    boolean getAutoRefreshToken();

    public Lock getAccessTokenLock();

    /**
     * 得到微信接口地址域名部分的自定义设置信息.
     */
    WxMpHostConfig getHostConfig();

}
