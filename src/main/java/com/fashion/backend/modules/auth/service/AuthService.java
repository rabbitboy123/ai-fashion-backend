package com.fashion.backend.modules.auth.service;

import com.fashion.backend.common.constant.RoleType;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.auth.dto.AuthResponse;
import com.fashion.backend.modules.auth.dto.LoginRequest;
import com.fashion.backend.modules.auth.dto.RegisterRequest;
import com.fashion.backend.modules.auth.dto.UserResponse;
import com.fashion.backend.modules.cart.entity.Cart;
import com.fashion.backend.modules.cart.repository.CartRepository;
import com.fashion.backend.modules.user.entity.User;
import com.fashion.backend.modules.user.repository.UserRepository;
import com.fashion.backend.security.JwtTokenProvider;
import com.fashion.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email không được để trống.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email '" + normalizedEmail + "' đã được sử dụng. Vui lòng chọn email khác.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .role(RoleType.ROLE_CUSTOMER)
                .build();

        User savedUser = userRepository.save(user);

        // Khởi tạo giỏ hàng trống cho tài khoản mới
        Cart cart = Cart.builder()
                .user(savedUser)
                .build();
        cartRepository.save(cart);

        UserPrincipal principal = UserPrincipal.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        String token = tokenProvider.generateToken(authentication);

        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole().name())
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().trim().toLowerCase(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            return AuthResponse.builder()
                    .accessToken(token)
                    .tokenType("Bearer")
                    .id(principal.getId())
                    .email(principal.getEmail())
                    .fullName(principal.getFullName())
                    .role(principal.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_CUSTOMER"))
                    .build();
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Email hoặc mật khẩu không chính xác.");
        }
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            throw new BadRequestException("Chưa đăng nhập.");
        }

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin tài khoản người dùng."));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
