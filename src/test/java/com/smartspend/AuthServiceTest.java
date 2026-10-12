package com.smartspend;

import com.smartspend.dto.AuthResponse;
import com.smartspend.dto.LoginRequest;
import com.smartspend.dto.RegisterRequest;
import com.smartspend.dto.UserResponse;
import com.smartspend.exception.BadRequestException;
import com.smartspend.repository.UserRepository;
import com.smartspend.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterSuccess() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("John Doe");
        req.setEmail("john@example.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");

        UserResponse response = authService.register(req);

        assertNotNull(response.getId());
        assertEquals("John Doe", response.getFullName());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("INR", response.getCurrency());
    }

    @Test
    void testRegisterPasswordMismatch() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Jane Doe");
        req.setEmail("jane@example.com");
        req.setPassword("Password123");
        req.setConfirmPassword("DifferentPassword");

        assertThrows(BadRequestException.class, () -> authService.register(req));
    }

    @Test
    void testRegisterDuplicateEmail() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("User One");
        req.setEmail("same@example.com");
        req.setPassword("Password123");
        req.setConfirmPassword("Password123");

        authService.register(req);

        RegisterRequest req2 = new RegisterRequest();
        req2.setFullName("User Two");
        req2.setEmail("same@example.com");
        req2.setPassword("Password123");
        req2.setConfirmPassword("Password123");

        assertThrows(BadRequestException.class, () -> authService.register(req2));
    }

    @Test
    void testLoginSuccess() {
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Alice Smith");
        reg.setEmail("alice@example.com");
        reg.setPassword("Secret123");
        reg.setConfirmPassword("Secret123");
        authService.register(reg);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("alice@example.com");
        loginReq.setPassword("Secret123");

        AuthResponse authResponse = authService.login(loginReq);

        assertNotNull(authResponse.getToken());
        assertEquals("Bearer", authResponse.getTokenType());
        assertEquals("alice@example.com", authResponse.getUser().getEmail());
    }

    @Test
    void testLoginInvalidCredentials() {
        RegisterRequest reg = new RegisterRequest();
        reg.setFullName("Bob Smith");
        reg.setEmail("bob@example.com");
        reg.setPassword("Secret123");
        reg.setConfirmPassword("Secret123");
        authService.register(reg);

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("bob@example.com");
        loginReq.setPassword("WrongPassword");

        assertThrows(BadCredentialsException.class, () -> authService.login(loginReq));
    }
}
