package com.duhao.security.checkinapp.impl;

import com.duhao.security.checkinapp.dto.WechatLoginRequest;
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
    public Map<String, String> wechatLogin(WechatLoginRequest req) {
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
                throw new RuntimeException("未注册保安，请联系管理员");
            }

            // 4. 绑定 openid（首次或变更时）
            if (!openid.equals(guard.getOpenId())) {
                guard.setOpenId(openid);
                guardRepository.save(guard);
            }

            // 5. 生成 JWT
            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            // 6. 返回结果
            return Map.of(
                    "token", token,
                    "name", guard.getName(),
                    "phone", guard.getPhoneNumber(),
                    "employeeId", guard.getEmployeeId()
            );
        } catch (WxErrorException e) {
            throw new RuntimeException("调用微信接口失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> wechatLaunch(WechatLoginRequest req) {
        try {
            // 验证openid是否已经有绑定了的账号
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(req.getLoginCode());
            String openid = sessionInfo.getOpenid();

            SecurityGuard guard = guardRepository.findByOpenId(openid);
            if (guard == null) {
                throw new RuntimeException("未搜索到openid");
            }

            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            return Map.of(
                    "token", token,
                    "name", guard.getName(),
                    "phone", guard.getPhoneNumber(),
                    "employeeId", guard.getEmployeeId()
            );
        } catch (WxErrorException e) {
            throw new RuntimeException("调用微信接口失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, String> wechatTokenRefresh(HttpServletRequest request){
        String BEARER_PREFIX = "Bearer ";

        // 1. 拿到 Authorization 头
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new RuntimeException("缺少或无效的 Authorization 头");
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
            throw new RuntimeException("token无效");
        }

        // 3. 从 subject 拆出 openid
        //    假设你之前用 "openid:xxx" 作为 subject
        String subject = claims.getSubject();
        if (subject == null || !subject.startsWith("openid:")) {
            throw new RuntimeException("token无效");
        }
        String openid = subject.substring("openid:".length());

        // 可选：再查库确认 openid 对应的用户存在
        SecurityGuard guard = guardRepository.findByOpenId(openid);
        if (guard == null) {
            throw new RuntimeException("不存在绑定该openid的用户");
        }

        // 4. 重新生成 Token（24–48h 随机过期）
        String newToken = jwtUtil.generateWechatToken(openid);

        // 5. 返回给前端
        return Map.of("token", newToken);
    }
}
