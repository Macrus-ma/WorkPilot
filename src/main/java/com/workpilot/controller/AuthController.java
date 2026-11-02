package com.workpilot.controller;

import com.workpilot.dto.LoginDTO;
import com.workpilot.dto.RegisterDTO;
import com.workpilot.security.CurrentUser;
import com.workpilot.service.AuthService;
import com.workpilot.vo.LoginVO;
import com.workpilot.vo.MessageVO;
import com.workpilot.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserVO> register(@Valid
                                                @RequestBody RegisterDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public LoginVO login(@Valid
                               @RequestBody LoginDTO request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public MessageVO logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization.substring(7));
        return new MessageVO("已退出登录");
    }

    @GetMapping("/me")
    public UserVO me(@AuthenticationPrincipal CurrentUser user) {
        return new UserVO(user.id(), user.loginName(), user.displayName(), user.userType());
    }
}
