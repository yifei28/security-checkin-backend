package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.dto.WechatLoginRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface WechatLoginService {
    Map<String, String> wechatLogin(WechatLoginRequest wechatLoginRequest);
    Map<String, String> wechatLaunch(WechatLoginRequest wechatLoginRequest);
    Map<String, String> wechatTokenRefresh(HttpServletRequest request);
}
