package com.wyb.mp.api.impl;

import java.io.File;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wyb.common.bean.WxJsapiSignature;
import com.wyb.common.exception.WxErrorException;
import com.wyb.common.util.RandomUtils;
import com.wyb.common.util.crypto.SHA1;
import com.wyb.common.util.http.HttpClientUtil;
import com.wyb.common.util.http.URIUtil;
import com.wyb.mp.api.*;
import com.wyb.mp.bean.result.WxMpOAuth2AccessToken;
import com.wyb.mp.bean.result.WxMpUser;
import com.wyb.mp.enums.TicketType;

/**
 * @author Kunzite
 */
@Slf4j
public abstract class BaseWxMpServiceImpl implements WxMpService {
    private static final JsonParser JSON_PARSER = new JsonParser();

    // 关联各个微信的api实现
    private WxMpTemplateMsgService wxMpTemplateMsgService = new WxMpTemplateMsgServiceImpl(this);
    private WxMpMassMessageService wxMpMassMessageService = new WxMpMassMessageServiceImpl(this);
    private WxMpUserTagServiceImpl wxMpUserTagService = new WxMpUserTagServiceImpl(this);
    private WxMpMenuService menuService = new WxMpMenuServiceImpl(this);

    // 微信配置
    protected WxMpConfigStorage wxMpConfigStorage;

    private int retrySleepMillis = 1000;
    private int maxRetryTimes = 5;

    @Override
    public boolean checkSignature(String timestamp, String nonce, String signature) {
        try {
            return SHA1.gen(this.getWxMpConfigStorage().getToken(), timestamp, nonce).equals(signature);
        }
        catch (Exception e) {
            log.error("Checking signature failed, reason {}" + e.getMessage());
        }
        return false;
    }

    @Override
    public String getTicket(TicketType type) throws WxErrorException {
        return this.getTicket(type, false);
    }

    @Override
    public String getAccessToken() throws WxErrorException {
        return getAccessToken(false);
    }

    @Override
    public String getTicket(TicketType type, boolean forceRefresh) throws WxErrorException {
        Lock lock = this.getWxMpConfigStorage().getTicketLock(type);
        try {
            lock.lock();
            if (forceRefresh) {
                this.getWxMpConfigStorage().expireTicket(type);
            }

            if (this.getWxMpConfigStorage().isTicketExpired(type)) {
                String responseContent = this.get(WxMpService.GET_TICKET_URL + type.getCode(), null);
                JsonObject tmpJsonObject = JSON_PARSER.parse(responseContent).getAsJsonObject();
                String jsapiTicket = tmpJsonObject.get("ticket").getAsString();
                int expiresInSeconds = tmpJsonObject.get("expires_in").getAsInt();
                this.getWxMpConfigStorage().updateTicket(type, jsapiTicket, expiresInSeconds);
            }
        }
        finally {
            lock.unlock();
        }

        return this.getWxMpConfigStorage().getTicket(type);
    }

    @Override
    public String getJsapiTicket() throws WxErrorException {
        return this.getJsapiTicket(false);
    }

    @Override
    public String getJsapiTicket(boolean forceRefresh) throws WxErrorException {
        return this.getTicket(TicketType.JSAPI, forceRefresh);
    }

    @Override
    public WxJsapiSignature createJsapiSignature(String url) throws WxErrorException {
        long timestamp = System.currentTimeMillis() / 1000;
        String randomStr = RandomUtils.getRandomStr();
        String jsapiTicket = getJsapiTicket(false);
        String signature = SHA1.genWithAmple("jsapi_ticket=" + jsapiTicket, "noncestr=" + randomStr,
                "timestamp=" + timestamp, "url=" + url);
        WxJsapiSignature jsapiSignature = new WxJsapiSignature();
        jsapiSignature.setAppId(this.getWxMpConfigStorage().getAppId());
        jsapiSignature.setTimestamp(timestamp);
        jsapiSignature.setNonceStr(randomStr);
        jsapiSignature.setUrl(url);
        jsapiSignature.setSignature(signature);
        return jsapiSignature;
    }

    @Override
    public String buildQrConnectUrl(String redirectURI, String scope, String state) {
        return String.format(WxMpService.QRCONNECT_URL, this.getWxMpConfigStorage().getAppId(),
                URIUtil.encodeURIComponent(redirectURI), scope, StringUtils.trimToEmpty(state));
    }

    /**
     * 构造oauth2授权的url连接. 即获取code
     */
    @Override
    public String oauth2buildAuthorizationUrl(String redirectURI, String scope, String state) {
        return String.format(CONNECT_OAUTH2_AUTHORIZE_URL, this.getWxMpConfigStorage().getAppId(),
                URIUtil.encodeURIComponent(redirectURI), scope, state);
    }

    private WxMpOAuth2AccessToken getOAuth2AccessToken(String url) throws WxErrorException {
        String response = HttpClientUtil.doGet(url);
        return WxMpOAuth2AccessToken.fromJson(response);
    }

    public WxMpOAuth2AccessToken oauth2getAccessToken(String code) throws WxErrorException {
        String url = String.format(WxMpService.OAUTH2_ACCESS_TOKEN_URL, this.getWxMpConfigStorage().getAppId(),
                this.getWxMpConfigStorage().getSecret(), code);
        return this.getOAuth2AccessToken(url);
    }

    @Override
    public WxMpUser oauth2getUserInfo(WxMpOAuth2AccessToken token) throws WxErrorException {
        String url = String.format(WxMpService.OAUTH2_USERINFO_URL, token.getAccessToken(), token.getOpenId(), null);
        String response = HttpClientUtil.doGet(url);
        return WxMpUser.fromJson(response);
    }

    @Override
    public String[] getCallbackIP() throws WxErrorException {
        String responseContent = HttpClientUtil.doGet(WxMpService.GET_CALLBACK_IP_URL, null);
        JsonElement tmpJsonElement = JSON_PARSER.parse(responseContent);
        JsonArray ipList = tmpJsonElement.getAsJsonObject().get("ip_list").getAsJsonArray();
        String[] ipArray = new String[ipList.size()];
        for (int i = 0; i < ipList.size(); i++) {
            ipArray[i] = ipList.get(i).getAsString();
        }
        return ipArray;
    }

    @Override
    public String get(String url, Map<String, String> params) throws WxErrorException {
        url += (url.contains("?") ? "&" : "?") + "access_token=" + getAccessToken(false);
        return HttpClientUtil.doGet(url, params);
    }

    @Override
    public String post(String url, Map<String, String> params) throws WxErrorException {
        url += (url.contains("?") ? "&" : "?") + "access_token=" + getAccessToken(false);
        return HttpClientUtil.doPost(url, params);
    }

    @Override
    public String post(String url, String jsonString) throws WxErrorException {
        url += (url.contains("?") ? "&" : "?") + "access_token=" + getAccessToken(false);
        return HttpClientUtil.doPostJson(url, jsonString);
    }

    @Override
    public String postFile(String url, File file) throws WxErrorException {
        url += (url.contains("?") ? "&" : "?") + "access_token=" + getAccessToken(false);
        return HttpClientUtil.doPostFile(url, file);
    }

    public void setRetrySleepMillis(int retrySleepMillis) {
        this.retrySleepMillis = retrySleepMillis;
    }

    public void setMaxRetryTimes(int maxRetryTimes) {
        this.maxRetryTimes = maxRetryTimes;
    }

    @Override
    public WxMpConfigStorage getWxMpConfigStorage() {
        return wxMpConfigStorage;
    }

    @Override
    public void setWxMpConfigStorage(WxMpConfigStorage wxMpConfigStorage) {
        this.wxMpConfigStorage = wxMpConfigStorage;
    }

    @Override
    public WxMpTemplateMsgService getWxMpTemplateMsgService() {
        return wxMpTemplateMsgService;
    }

    @Override
    public WxMpMassMessageService getWxMpMassMessageService() {
        return wxMpMassMessageService;
    }

    @Override
    public WxMpUserTagService getWxMpUserTagService() {
        return wxMpUserTagService;
    }

    @Override
    public WxMpMenuService getMenuService() {
        return menuService;
    }
}
