package com.workpilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.workpilot.common.ApiException;
import com.workpilot.dto.LoginDTO;
import com.workpilot.dto.RegisterDTO;
import com.workpilot.entity.AuthTokenEntity;
import com.workpilot.entity.UserEntity;
import com.workpilot.mapper.AuthTokenMapper;
import com.workpilot.mapper.UserMapper;
import com.workpilot.security.CurrentUser;
import com.workpilot.service.AuthService;
import com.workpilot.vo.LoginVO;
import com.workpilot.vo.UserVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final AuthTokenMapper tokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Clock clock = Clock.systemUTC();
    private final long tokenHours;

    public AuthServiceImpl(UserMapper userMapper, AuthTokenMapper tokenMapper,
                           PasswordEncoder passwordEncoder,
                           @Value("${workpilot.auth.token-hours}") long tokenHours) {
        this.userMapper = userMapper;
        this.tokenMapper = tokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenHours = tokenHours;
    }

    @Override
    @Transactional
    public UserVO register(RegisterDTO request) {
        validatePasswordLength(request.password());
        if (userMapper.selectCount(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getLoginName, request.loginName())) > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "登录名已存在");
        }
        UserEntity user = new UserEntity();
        user.setLoginName(request.loginName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName());
        user.setUserType("CUSTOMER");
        user.setStatus("ACTIVE");
        userMapper.insert(user);
        return new UserVO(user.getId(), user.getLoginName(),
                user.getDisplayName(), user.getUserType());
    }

    @Override
    @Transactional
    public LoginVO login(LoginDTO request) {
        validatePasswordLength(request.password());
        UserEntity user = userMapper.selectOne(Wrappers.<UserEntity>lambdaQuery()
                .eq(UserEntity::getLoginName, request.loginName()));
        if (user == null || !"ACTIVE".equals(user.getStatus())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        AuthTokenEntity token = new AuthTokenEntity();
        token.setUserId(user.getId());
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now(clock).plusHours(tokenHours));
        tokenMapper.insert(token);
        return new LoginVO(rawToken, token.getExpiresAt());
    }

    @Override
    public CurrentUser authenticate(String rawToken) {
        if (rawToken == null || rawToken.length() > 128 || rawToken.isBlank()) {
            return null;
        }
        AuthTokenEntity token = tokenMapper.selectOne(Wrappers.<AuthTokenEntity>lambdaQuery()
                .eq(AuthTokenEntity::getTokenHash, hash(rawToken)));
        if (token == null || token.getRevokedAt() != null
                || !token.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
            return null;
        }
        UserEntity user = userMapper.selectById(token.getUserId());
        if (user == null || !"ACTIVE".equals(user.getStatus())) {
            return null;
        }
        return toCurrentUser(user);
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        tokenMapper.update(null, Wrappers.<AuthTokenEntity>lambdaUpdate()
                .set(AuthTokenEntity::getRevokedAt, LocalDateTime.now(clock))
                .eq(AuthTokenEntity::getTokenHash, hash(rawToken))
                .isNull(AuthTokenEntity::getRevokedAt));
    }

    private CurrentUser toCurrentUser(UserEntity user) {
        return new CurrentUser(user.getId(), user.getLoginName(),
                user.getDisplayName(), user.getUserType());
    }

    private void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码的 UTF-8 长度不能超过 72 字节");
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
