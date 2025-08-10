package com.duhao.security.checkinapp.service;

import com.duhao.security.checkinapp.dto.WechatLoginRequest;
import com.duhao.security.checkinapp.dto.WechatLoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface WechatLoginService {
    WechatLoginResponse wechatLogin(WechatLoginRequest wechatLoginRequest);
    WechatLoginResponse wechatLaunch(WechatLoginRequest wechatLoginRequest);
    WechatLoginResponse wechatTokenRefresh(HttpServletRequest request);
}
