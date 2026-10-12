package com.smartspend.service;

import com.smartspend.dto.ChangePasswordRequest;
import com.smartspend.dto.UpdateProfileRequest;
import com.smartspend.dto.UserResponse;
import com.smartspend.entity.User;
import com.smartspend.exception.BadRequestException;
import com.smartspend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(AuthService authService,
                           UserRepository userRepository,
                           PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getSettings() {
        return authService.getCurrentUser();
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = authService.getCurrentUserEntity();
        user.setFullName(request.getFullName().trim());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            user.setCurrency(request.getCurrency().trim().toUpperCase());
        }
        User saved = userRepository.save(user);
        return authService.mapToUserResponse(saved);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = authService.getCurrentUserEntity();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("New password cannot be the same as current password");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
