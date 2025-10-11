package com.example.springoauth2profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public OAuth2User profile(@AuthenticationPrincipal OAuth2User principal) {
        return principal;
    }

    @GetMapping("/debug/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/test-oauth")
    public String testOAuth() {
        return "OAuth test endpoint - if you can see this, authentication is working";
    }
}
