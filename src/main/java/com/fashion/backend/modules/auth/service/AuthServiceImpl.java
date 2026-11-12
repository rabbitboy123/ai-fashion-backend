package com.fashion.backend.modules.auth.service;

import com.fashion.backend.common.constant.RoleType;
import com.fashion.backend.common.exception.BadRequestException;
import com.fashion.backend.common.exception.ResourceNotFoundException;
import com.fashion.backend.modules.auth.dto.response.AuthResponse;
import com.fashion.backend.modules.auth.dto.request.LoginRequest;
import com.fashion.backend.modules.auth.dto.request.RegisterRequest;
import com.fashion.backend.modules.user.dto.response.UserResponse;
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
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email() != null ? request.email().trim().toLowerCase() : "";
        if (normalizedEmail.isEmpty()) {
            throw new BadRequestException("Email không được để trống.");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BadRequestException("Email '" + normalizedEmail + "' đã được sử dụng. Vui lòng chọn email khác.");
        }

        User user = User.builder()
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName().trim())
                .gender(request.gender())
                .dateOfBirth(request.dateOfBirth())
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

        return new AuthResponse(
                token,
                "Bearer",
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().trim().toLowerCase(),
                            request.password()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String token = tokenProvider.generateToken(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

            return new AuthResponse(
                    token,
                    "Bearer",
                    principal.getId(),
                    principal.getEmail(),
                    principal.getFullName(),
                    principal.getAuthorities().stream().findFirst().map(Object::toString).orElse("ROLE_CUSTOMER")
            );
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

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getGender(),
                user.getDateOfBirth(),
                user.getCreatedAt()
        );
    }
}
