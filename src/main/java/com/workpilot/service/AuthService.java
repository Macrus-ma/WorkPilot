package com.workpilot.service;

import com.workpilot.dto.LoginDTO;
import com.workpilot.dto.RegisterDTO;
import com.workpilot.security.CurrentUser;
import com.workpilot.vo.LoginVO;
import com.workpilot.vo.UserVO;

public interface AuthService {
    UserVO register(RegisterDTO request);
    LoginVO login(LoginDTO request);
    CurrentUser authenticate(String token);
    void logout(String token);
}
