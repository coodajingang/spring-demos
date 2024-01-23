package demo.ss.filter;

import cn.hutool.json.JSONUtil;
import demo.ss.exception.MFAInvalideException;
import demo.ss.exception.NotMatchAuthenException;
import demo.ss.users.CustomUserDetails;
import demo.ss.utils.TimeBasedOneTimePasswordUtil;
import demo.ss.utils.ZxingPngQrGenerator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.log.LogMessage;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

//@Lazy
@Slf4j
//@Component
public class MFAFilter extends GenericFilterBean {
    private static final String LOGIN_URL = "/oauth/token";
    private static final String BIND_URL = "/mfa/bind";
    private static final String BIND_PRE_URL = "/mfa/check";

    private static final AntPathRequestMatcher LOGIN_REQUEST_MATCHER = new AntPathRequestMatcher(LOGIN_URL, "POST");
    private static final AntPathRequestMatcher BIND_REQUEST_MATCHER = new AntPathRequestMatcher(BIND_URL, "POST");
    private static final AntPathRequestMatcher PRE_BIND_REQUEST_MATCHER = new AntPathRequestMatcher(BIND_PRE_URL, "POST");


    private ClientDetailsService clientDetailsService;
    private UserDetailsService userDetailsService;
    private PasswordEncoder passwordEncoder;

    private AuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();

    public MFAFilter(ClientDetailsService clientDetailsService, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.clientDetailsService = clientDetailsService;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        if (!req.getMethod().equals("POST")) {
            chain.doFilter(request, response);
            return;
        }

        if (LOGIN_REQUEST_MATCHER.matches(req)) {
            // check mfa-code , success continue , error return response
            log.debug("MFA Filter ， check loging code ");
            String grantType = this.obtainGrantType(req);
            if (!"password".equals(grantType)) {
                chain.doFilter(request, response);
                return;
            }

            String username = this.obtainUsername(req);
            String mfaCode = this.obtainMfaCode(req);

            try {
                String secret = checkAndGetMfaInfo(username);
                checkMfaCode(secret, mfaCode);
            } catch (AuthenticationException exp) {
                // fail and send response
                log.error("MFA 校验失败 {} {} {}", username, mfaCode, exp.getMessage());
                this.unsuccessfulAuthentication(req, (HttpServletResponse) response, exp);
                return;
            } catch (Exception exp) {
                log.error("MFA 校验失败 {} {} 异常", username, mfaCode, exp);
                this.unsuccessfulAuthentication(req, (HttpServletResponse) response, new AuthenticationServiceException(exp.getMessage()));
                return;
            }

            log.debug("MFA 校验 code success! {}", username);
            chain.doFilter(request, response);
        } else if (BIND_REQUEST_MATCHER.matches(req)) {
            // check client , check user, check mfa-code , update mfa status , return response
            log.debug("MFA Filter ， bind mfa devices");
            String username = this.obtainUsername(req);
            String password = this.obtainPassword(req);
            String mfaCode = this.obtainMfaCode(req);
            String clientId = this.obtainClientId(req);

            try {
                checkClient(clientId);
                CustomUserDetails userDetails = checkUsername(username, password);
                checkMfaStatusWaitBind(userDetails);
                checkMfaCode(userDetails.getMfaSecret(), mfaCode);
                //TODO updateUserMfa(username);
                Map<String, Object> data = new HashMap<>();
                successJsonResonse(response, data);
            } catch (Exception exp) {
                failJsonResponse(response, exp.getMessage());
            }

        } else if (PRE_BIND_REQUEST_MATCHER.matches(req)) {
            // check client , check user , generate qr images , update mfa status and securet , return response
            log.debug("MFA Filter ， prebind mfa devices");
            String username = this.obtainUsername(req);
            String clientId = this.obtainClientId(req);
            String password = this.obtainPassword(req);

            try {
                checkClient(clientId);
                CustomUserDetails userDetails = checkUsername(username, password);

                checkMfaStatus(userDetails);

                String mfaSecret = TimeBasedOneTimePasswordUtil.generateBase32Secret();
                final String qrData = TimeBasedOneTimePasswordUtil.generateOtpAuthUrlOri(username + "@OM", mfaSecret);
                String qrImg = ZxingPngQrGenerator.generateBase64QRPng(qrData);
                //TODO updateUserMfa(username);
                Map<String, Object> data = new HashMap<>();
                data.put("qr", qrImg);
                successJsonResonse(response, data);
            } catch (Exception exp) {
                failJsonResponse(response, exp.getMessage());
            }

        } else {
            if (this.logger.isTraceEnabled()) {
                this.logger.trace(LogMessage.format("Did not match MFA filter request %s", req.getRequestURI()));
            }
            chain.doFilter(request, response);
        }
    }

    private String checkAndGetMfaInfo(String username) {
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            log.error("{} 's user info is null ", username);
            throw new UsernameNotFoundException(username);
        }

        if (!(userDetails instanceof CustomUserDetails)) {
            log.error("Error! 非CustomUserDetails实例");
            throw new MFAInvalideException("非CustomUserDetails实例");
        }

        CustomUserDetails user = (CustomUserDetails) userDetails;
        if (user.getMfaStatus() != 2 || user.getMfaSecret() == null || user.getMfaSecret().length() == 0) {
            log.error("当前用户为绑定OPT 请绑定后登陆 {} {} {} ", username, user.getMfaStatus(), user.getMfaSecret());
            throw new MFAInvalideException("当前用户为绑定OPT 请绑定后登陆");
        }
        return user.getMfaSecret();
    }

    private void checkMfaStatusWaitBind(CustomUserDetails user) {
        checkMfaStatus(user);
        if (user.getMfaStatus() != 1) {
            log.error("MFA 状态非待绑定 请重新进行验证绑定 {}", user.getUsername());
            throw new MFAInvalideException("状态非待绑定 请重新进行验证绑定");
        }
    }

    private void checkMfaStatus(CustomUserDetails user) {

        if (user.getMfaStatus() == 2 && user.getMfaSecret() != null) {
            log.error("MFA 已经绑定，不允许重复绑定 {}", user.getUsername());
            throw new MFAInvalideException("已经绑定，不允许重复绑定");
        }
    }


    private CustomUserDetails checkUsername(String username, String password) {
        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (userDetails == null) {
            log.error("{} 's user info is null ", username);
            throw new UsernameNotFoundException(username);
        }

        final boolean matches = passwordEncoder.matches(password, userDetails.getPassword());
        if (!matches) {
            log.error("密码错误 {}", username);
            throw new BadCredentialsException("密码错误");
        }

        if (userDetails instanceof CustomUserDetails) {
            return (CustomUserDetails) userDetails;
        }

        log.error("Error! 非CustomUserDetails实例");
        throw new MFAInvalideException("非CustomUserDetails实例");
    }

    private void checkClient(String clientId) {
        final ClientDetails clientDetails = clientDetailsService.loadClientByClientId(clientId);

        if (clientDetails == null || !clientDetails.getClientId().equals(clientId)) {
            log.error("MFA 校验 clientId 不匹配 {}", clientId);
            throw new NotMatchAuthenException("clientId不匹配");
        }

    }

    @SneakyThrows
    private void successJsonResonse(ServletResponse response, Map<String, Object> data) {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("msg", "success");
        map.put("data", data);
        PrintWriter out = response.getWriter();
        out.print(JSONUtil.toJsonStr(map));
        out.flush();
    }

    private void checkMfaCode(String secret, String mfaCode) {
        if (secret == null || secret.length() == 0) {
            log.error("MFA绑定迷药为空");
            throw new MFAInvalideException("MFA密钥为空");
        }
        if (mfaCode == null || mfaCode.length() == 0) {
            log.error("MFA 请输入OTP code");
            throw new MFAInvalideException("请输入OTP code");
        }
        try {
            final boolean b = TimeBasedOneTimePasswordUtil.validateCurrentNumber(secret, Integer.valueOf(mfaCode), 3000);
            if (!b) {
                log.error("校验OTP code 不匹配 {} {}", secret, mfaCode);
                throw new MFAInvalideException("校验OTP code 不匹配");
            }
        } catch (GeneralSecurityException e) {
            log.error("校验OTP异常", e);
            throw new MFAInvalideException("校验OTP异常", e);
        }

    }

    @SneakyThrows
    private void failJsonResponse(ServletResponse response, String msg) {
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> map = new HashMap<>();
        map.put("code", 400);
        map.put("msg", msg);
        PrintWriter out = response.getWriter();
        out.print(JSONUtil.toJsonStr(map));
        out.flush();
    }


    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        this.logger.trace("Failed to process authentication request", failed);
        this.logger.trace("Cleared SecurityContextHolder");
        this.logger.trace("Handling authentication failure");
        this.failureHandler.onAuthenticationFailure(request, response, failed);
    }
    private String trim(String str) {
        if (str == null) return "";
        return str.trim();
    }

    @Nullable
    protected String obtainUsername(HttpServletRequest request) {
        return obtainParameter(request, "username");
    }

    @Nullable
    protected String obtainPassword(HttpServletRequest request) {
        return obtainParameter(request, "password");
    }

    @Nullable
    protected String obtainGrantType(HttpServletRequest request) {
        return obtainParameter(request, "grant_type");
    }

    @Nullable
    protected String obtainMfaCode(HttpServletRequest request) {
        return obtainParameter(request, "mfa_code");
    }

    @Nullable
    protected String obtainClientId(HttpServletRequest request) {
        return obtainParameter(request, "client_id");
    }

    @Nullable
    protected String obtainClientSecret(HttpServletRequest request) {
        return obtainParameter(request, "secret");
    }

    @Nullable
    private String obtainParameter(HttpServletRequest request, String parameter) {
        return trim(request.getParameter(parameter));
    }

    public void setAuthenticationFailureHandler(AuthenticationFailureHandler failureHandler) {
        Assert.notNull(failureHandler, "failureHandler cannot be null");
        this.failureHandler = failureHandler;
    }

    protected AuthenticationFailureHandler getFailureHandler() {
        return this.failureHandler;
    }
}
