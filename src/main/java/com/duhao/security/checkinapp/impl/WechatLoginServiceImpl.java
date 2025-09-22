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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 微信登录服务实现（使用官方 WxJava SDK）
 */
@Service
public class WechatLoginServiceImpl implements WechatLoginService {

    private static final Logger logger = LoggerFactory.getLogger(WechatLoginServiceImpl.class);

    @Autowired
    private WxMaService wxMaService;

    @Autowired
    private SecurityGuardRepository guardRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public WechatLoginResponse wechatLogin(WechatLoginRequest req) {
        logger.info("开始处理微信登录 - loginCode前6位: {}", 
                   req.getLoginCode() != null ? req.getLoginCode().substring(0, Math.min(6, req.getLoginCode().length())) + "..." : "null");
        try {
            // 1. 用同一个 loginCode 换 session_key + openid
            logger.debug("调用微信API获取session信息");
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(req.getLoginCode());
            String openid = sessionInfo.getOpenid();
            logger.info("获取到openid: {}", openid.substring(0, Math.min(8, openid.length())) + "...");

            // 2. 用 phoneCode 换手机号（基础库2.21.2+ 或 2023-08-28+）
            logger.debug("调用微信API获取手机号信息");
            WxMaPhoneNumberInfo phoneInfo =
                    wxMaService.getUserService().getPhoneNumber(req.getPhoneCode());
            String phone = phoneInfo.getPhoneNumber();
            logger.info("获取到手机号: {}****{}", phone.substring(0, 3), phone.substring(phone.length() - 4));

            // 3. 根据手机号查库
            logger.debug("查询手机号对应的保安信息");
            SecurityGuard guard = guardRepository.findByPhoneNumber(phone);
            if (guard == null) {
                logger.warn("手机号未注册: {}****{}", phone.substring(0, 3), phone.substring(phone.length() - 4));
                return WechatLoginResponse.error("未注册保安，请联系管理员", "40001");
            }
            logger.info("找到保安: {} (工号: {})", guard.getName(), guard.getEmployeeId());

            // 4. 绑定 openid（首次或变更时）
            if (!openid.equals(guard.getOpenId())) {
                logger.info("更新保安openid绑定: {} -> {}", 
                           guard.getOpenId() != null ? guard.getOpenId().substring(0, Math.min(8, guard.getOpenId().length())) + "..." : "null",
                           openid.substring(0, Math.min(8, openid.length())) + "...");
                guard.setOpenId(openid);
                guardRepository.save(guard);
            } else {
                logger.debug("openid已绑定，无需更新");
            }

            // 5. 生成 JWT
            logger.debug("生成JWT Token");
            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            // 6. 构建用户信息
            WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo(
                    openid,
                    guard.getName(),
                    guard.getEmployeeId(),
                    guard.getPhoneNumber(),
                    guard.getSite() != null ? guard.getSite().getName() : null,
                    guard.getRole() != null ? guard.getRole().getDisplayName() : null
            );

            // 7. 返回标准格式
            logger.info("微信登录成功完成 - 工号: {}, openid: {}", 
                       guard.getEmployeeId(), openid.substring(0, Math.min(8, openid.length())) + "...");
            return WechatLoginResponse.success(token, userInfo, "Phone authorization successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (WxErrorException e) {
            String errorCode = e.getError() != null ? String.valueOf(e.getError().getErrorCode()) : "40029";
            logger.error("微信API调用失败 - 错误码: {}, 错误信息: {}", errorCode, e.getMessage(), e);
            return WechatLoginResponse.error("调用微信接口失败: " + e.getMessage(), errorCode);
        } catch (Exception e) {
            logger.error("微信登录系统内部错误", e);
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }

    @Override
    public WechatLoginResponse wechatLaunch(WechatLoginRequest req) {
        logger.info("开始处理微信启动登录 - loginCode前6位: {}", 
                   req.getLoginCode() != null ? req.getLoginCode().substring(0, Math.min(6, req.getLoginCode().length())) + "..." : "null");
        try {
            // 验证openid是否已经有绑定了的账号
            logger.debug("调用微信API获取session信息");
            WxMaJscode2SessionResult sessionInfo =
                    wxMaService.getUserService().getSessionInfo(req.getLoginCode());
            String openid = sessionInfo.getOpenid();
            logger.info("获取到openid: {}", openid.substring(0, Math.min(8, openid.length())) + "...");

            logger.debug("查询openid对应的保安信息");
            SecurityGuard guard = guardRepository.findByOpenId(openid);
            if (guard == null) {
                logger.warn("openid未绑定任何保安账号: {}", openid.substring(0, Math.min(8, openid.length())) + "...");
                return WechatLoginResponse.error("用户未注册或openid未绑定", "40002");
            }
            logger.info("找到绑定保安: {} (工号: {})", guard.getName(), guard.getEmployeeId());

            logger.debug("生成JWT Token");
            String token = jwtUtil.generateWechatToken(guard.getOpenId());

            // 构建用户信息
            WechatLoginResponse.UserInfo userInfo = new WechatLoginResponse.UserInfo(
                    openid,
                    guard.getName(),
                    guard.getEmployeeId(),
                    guard.getPhoneNumber(),
                    guard.getSite() != null ? guard.getSite().getName() : null,
                    guard.getRole() != null ? guard.getRole().getDisplayName() : null
            );

            logger.info("微信启动登录成功完成 - 工号: {}, openid: {}", 
                       guard.getEmployeeId(), openid.substring(0, Math.min(8, openid.length())) + "...");
            return WechatLoginResponse.success(token, userInfo, "Login successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (WxErrorException e) {
            String errorCode = e.getError() != null ? String.valueOf(e.getError().getErrorCode()) : "40029";
            logger.error("微信启动登录API调用失败 - 错误码: {}, 错误信息: {}", errorCode, e.getMessage(), e);
            return WechatLoginResponse.error("调用微信接口失败: " + e.getMessage(), errorCode);
        } catch (Exception e) {
            logger.error("微信启动登录系统内部错误", e);
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }

    @Override
    public WechatLoginResponse wechatTokenRefresh(HttpServletRequest request){
        String BEARER_PREFIX = "Bearer ";
        
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        logger.info("开始处理微信Token刷新请求 - Token前缀: {}", 
                   authHeader != null && authHeader.length() > 13 ? authHeader.substring(0, 13) + "..." : "null");

        try {
            // 1. 拿到 Authorization 头
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                logger.warn("缺少或无效的Authorization头");
                return WechatLoginResponse.error("缺少或无效的 Authorization 头", "40003");
            }
            String oldToken = authHeader.substring(BEARER_PREFIX.length());
            logger.debug("解析出旧Token，长度: {}", oldToken.length());

            Claims claims;
            try {
                // 2a. 如果还没过期，正常解析
                logger.debug("尝试解析JWT Token");
                claims = jwtUtil.getClaims(oldToken);
                logger.debug("Token未过期，成功解析");
            } catch (ExpiredJwtException ex) {
                // 2b. 如果过期，还是能从异常里拿到 claims
                logger.info("Token已过期，从异常中获取claims");
                claims = ex.getClaims();
            } catch (JwtException ex) {
                // 其它错误（签名错、格式错）都视为无效
                logger.warn("Token解析失败: {}", ex.getMessage());
                return WechatLoginResponse.error("token无效", "40004");
            }

            // 3. 从 subject 拆出 openid
            String subject = claims.getSubject();
            if (subject == null || !subject.startsWith("openid:")) {
                logger.warn("Token格式无效，subject: {}", subject);
                return WechatLoginResponse.error("token格式无效", "40005");
            }
            String openid = subject.substring("openid:".length());
            logger.info("从Token中提取openid: {}", openid.substring(0, Math.min(8, openid.length())) + "...");

            // 可选：再查库确认 openid 对应的用户存在
            logger.debug("验证openid对应的保安是否存在");
            SecurityGuard guard = guardRepository.findByOpenId(openid);
            if (guard == null) {
                logger.warn("openid对应的保安不存在: {}", openid.substring(0, Math.min(8, openid.length())) + "...");
                return WechatLoginResponse.error("不存在绑定该openid的用户", "40006");
            }
            logger.info("验证通过，保安: {} (工号: {})", guard.getName(), guard.getEmployeeId());

            // 4. 重新生成 Token（2小时过期）
            logger.debug("生成新的JWT Token");
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
            logger.info("微信Token刷新成功完成 - 工号: {}, openid: {}", 
                       guard.getEmployeeId(), openid.substring(0, Math.min(8, openid.length())) + "...");
            return WechatLoginResponse.success(newToken, userInfo, "Token refresh successful", jwtUtil.getWechatTokenExpirationInSeconds());
            
        } catch (Exception e) {
            logger.error("微信Token刷新系统内部错误", e);
            return WechatLoginResponse.error("系统内部错误", "50000");
        }
    }
}
