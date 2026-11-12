package com.fashion.backend.modules.auth.service;

import com.fashion.backend.modules.auth.dto.response.AuthResponse;
import com.fashion.backend.modules.auth.dto.request.LoginRequest;
import com.fashion.backend.modules.auth.dto.request.RegisterRequest;
import com.fashion.backend.modules.user.dto.response.UserResponse;
import com.fashion.backend.security.UserPrincipal;

public interface AuthService {
    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse getCurrentUser(UserPrincipal userPrincipal);
}