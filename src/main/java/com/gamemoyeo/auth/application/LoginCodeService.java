package com.gamemoyeo.auth.application;

import com.gamemoyeo.auth.application.port.out.LoginCodePort;
import com.gamemoyeo.common.exception.ApiException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoginCodeService {

    private final LoginCodePort loginCodePort;
    private final Duration ttl;
    private final SecureRandom secureRandom = new SecureRandom();

    public LoginCodeService(
        LoginCodePort loginCodePort,
        @Value("${app.social-login.login-code-ttl}") Duration ttl
    ) {
        this.loginCodePort = loginCodePort;
        this.ttl = ttl;
    }

    public String issue(long memberId) {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        loginCodePort.save(code, memberId, ttl);
        return code;
    }

    public long consume(String code) {
        return loginCodePort.consume(code).orElseThrow(() -> new ApiException(
            HttpStatus.UNAUTHORIZED, "INVALID_LOGIN_CODE", "Login code is invalid or expired."));
    }
}
