package com.duhao.security.checkinapp.impl;

import com.duhao.security.checkinapp.dto.WechatLoginRequest;
import com.duhao.security.checkinapp.dto.WechatLoginResponse;
import com.duhao.security.checkinapp.entity.SecurityGuard;
import com.duhao.security.checkinapp.repository.SecurityGuardRepository;
import com.duhao.security.checkinapp.service.WechatLoginService;
import com.duhao.security.checkinapp.util.JwtUtil;
import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 微信登录服务实现（使用官方 WxJava SDK）
 */
@Service
public class WechatLoginServiceImpl implements WechatLoginService {

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private SecurityGuardRepository guardRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public WechatLoginResponse wechatLogin(WechatLoginRequest req) {
        try {
            // 1. 用同一个 loginCode 换 session_key + openid
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(req.getLoginCode());
            String openid = sessionInfo.getOpenid();

            // 2. 用 phoneCode 换手机号（基础库2.21.2+ 或 2023-08-28+）
            WxMaPhoneNumberInfo phoneInfo =
                    wxMaService.getUserService().getPhoneNumber(req.getPhoneCode());
            String phone = phoneInfo.getPhoneNumber();

            // 3. 根据手机号查库
            SecurityGuard guard = guardRepository.findByPhoneNumber(phone);
            if (guard == null) {
                return WechatLoginResponse.error("未注册保安，请联系管理员", "40001");
            }

            // 4. 绑定 openid（首次或变更时）
            if (!openid.equals(guard.getOpenId())) {
                guard.setOpenId(openid);
                guardRepository.save(guard);
            }

            // 5. 生成 JWT
            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            // 6. 构建用户信息
            WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo(
                    openid,
                    guard.getName(),
                    guard.getEmployeeId(),
                    guard.getPhoneNumber(),
                    guard.getSite() != null ? guard.getSite().getName() : null
            );

            // 7. 返回标准格式
            return WechatLoginResponse.success(token, userInfo, "Phone authorization successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (WxErrorException e) {
            String errorCode = e.getError() != null ? String.valueOf(e.getError().getErrorCode()) : "40029";
            return WechatLoginResponse.error("调用微信接口失败: " + e.getMessage(), errorCode);
        } catch (Exception e) {
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }

    @Override
    public WechatLoginResponse wechatLaunch(WechatLoginRequest req) {
        try {
            // 验证openid是否已经有绑定了的账号
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(req.getLoginCode());
            String openid = sessionInfo.getOpenid();

            SecurityGuard guard = guardRepository.findByOpenId(openid);
            if (guard == null) {
                return WechatLoginResponse.error("用户未注册或openid未绑定", "40002");
            }

            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            // 构建用户信息
            WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo(
                    openid,
                    guard.getName(),
                    guard.getEmployeeId(),
                    guard.getPhoneNumber(),
                    guard.getSite() != null ? guard.getSite().getName() : null
            );

            return WechatLoginResponse.success(token, userInfo, "Login successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (WxErrorException e) {
            String errorCode = e.getError() != null ? String.valueOf(e.getError().getErrorCode()) : "40029";
            return WechatLoginResponse.error("调用微信接口失败: " + e.getMessage(), errorCode);
        } catch (Exception e) {
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }

    @Override
    public WechatLoginResponse wechatTokenRefresh(HttpServletRequest request){
        String BEARER_PREFIX = "Bearer ";

        try {
            // 1. 拿到 Authorization 头
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return WechatLoginResponse.error("缺少或无效的 Authorization 头", "40003");
            }
            String oldToken = authHeader.substring(BEARER_PREFIX.length());

            Claims claims;
            try {
                // 2a. 如果还没过期，正常解析
                claims = jwtUtil.getClaims(oldToken);
            } catch (ExpiredJwtException ex) {
                // 2b. 如果过期，还是能从异常里拿到 claims
                claims = ex.getClaims();
            } catch (JwtException ex) {
                // 其它错误（签名错、格式错）都视为无效
                return WechatLoginResponse.error("token无效", "40004");
            }

            // 3. 从 subject 拆出 openid
            String subject = claims.getSubject();
            if (subject == null || !subject.startsWith("openid:")) {
                return WechatLoginResponse.error("token格式无效", "40005");
            }
            String openid = subject.substring("openid:".length());

            // 可选：再查库确认 openid 对应的用户存在
            SecurityGuard guard = guardRepository.findByOpenId(openid);
            if (guard == null) {
                return WechatLoginResponse.error("不存在绑定该openid的用户", "40006");
            }

            // 4. 重新生成 Token（2小时过期）
            String newToken = jwtUtil.generateWechatToken(openid);

            // 5. 构建用户信息
            WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo(
                    openid,
                    guard.getName(),
                    guard.getEmployeeId(),
                    guard.getPhoneNumber(),
                    guard.getSite() != null ? guard.getSite().getName() : null
            );

            // 6. 返回标准格式
            return WechatLoginResponse.success(newToken, userInfo, "Token refresh successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (Exception e) {
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }
}
