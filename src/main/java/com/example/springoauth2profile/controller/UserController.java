package com.example.springoauth2profile.controller;

import com.example.springoauth2profile.dto.ProfileUpdateRequest;
import com.example.springoauth2profile.model.User;
import com.example.springoauth2profile.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

@Controller
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping({"/profile", "/profile/"})
    @ResponseBody
    public User profile(@AuthenticationPrincipal OAuth2User principal) {
        System.out.println("============================================");
        System.out.println("🔍 /profile endpoint called!");
        System.out.println("🔍 Principal: " + (principal != null ? principal.getName() : "NULL"));
        
        if (principal == null) {
            System.out.println("❌ Principal is NULL!");
            throw new RuntimeException("Not authenticated");
        }
        
        String email = getEmailFromPrincipal(principal);
        System.out.println("🔍 Email extracted: " + email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        System.out.println("✅ User found: " + user.getEmail());
        System.out.println("============================================");
        return user;
    }

    @GetMapping("/debug/users")
    @ResponseBody
    public String getAllUsers() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ALL USERS IN DATABASE ===\n\n");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            sb.append("User ID: ").append(user.getId()).append("\n");
            sb.append("Email: ").append(user.getEmail()).append("\n");
            sb.append("Display Name: ").append(user.getDisplayName()).append("\n");
            sb.append("Created At: ").append(user.getCreatedAt()).append("\n");
            sb.append("---\n");
        }
        return sb.toString();
    }

    @PostMapping("/profile")
    @ResponseBody
    public User updateProfile(@AuthenticationPrincipal OAuth2User principal,
                            @RequestBody ProfileUpdateRequest request) {
        System.out.println("============================================");
        System.out.println("📝 UPDATE PROFILE called!");
        System.out.println("📝 Request - Display Name: " + request.getDisplayName());
        System.out.println("📝 Request - Bio: " + request.getBio());
        
        String email = getEmailFromPrincipal(principal);
        System.out.println("📝 User email: " + email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        System.out.println("📝 BEFORE update - Display Name: " + user.getDisplayName());
        System.out.println("📝 BEFORE update - Bio: " + user.getBio());
        
        user.setDisplayName(request.getDisplayName());
        user.setBio(request.getBio());
        
        System.out.println("📝 AFTER setters - Display Name: " + user.getDisplayName());
        System.out.println("📝 AFTER setters - Bio: " + user.getBio());

        User savedUser = userRepository.save(user);
        
        System.out.println("📝 SAVED - Display Name: " + savedUser.getDisplayName());
        System.out.println("📝 SAVED - Bio: " + savedUser.getBio());
        System.out.println("📝 SAVED - Updated At: " + savedUser.getUpdatedAt());
        System.out.println("✅ Profile update completed!");
        System.out.println("============================================");
        
        return savedUser;
    }

    // HTML Profile Page (for web browsers)
    @GetMapping("/profile/view")
    public String profileView() {
        return "profile";  // Changed from "profile-new" to "profile"
    }

    @GetMapping("/test-oauth")
    @ResponseBody
    public String testOAuth(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return "ERROR: Principal is NULL!";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== OAuth Principal Test ===\n\n");
        sb.append("Principal Class: ").append(principal.getClass().getName()).append("\n");
        sb.append("Principal Name: ").append(principal.getName()).append("\n\n");
        sb.append("Attributes:\n");
        principal.getAttributes().forEach((key, value) -> 
            sb.append("  ").append(key).append(": ").append(value).append("\n")
        );
        sb.append("\nEmail extraction test:\n");
        String email = getEmailFromPrincipal(principal);
        sb.append("  Extracted email: ").append(email).append("\n");
        
        return sb.toString();
    }

    /**
     * Helper method to extract email from OAuth2User principal
     * Uses the same logic as CustomOAuth2UserService for consistency
     */
    private String getEmailFromPrincipal(OAuth2User principal) {
        String email = principal.getAttribute("email");
        Map<String, Object> attributes = principal.getAttributes();
        
        System.out.println("🔍 getEmailFromPrincipal - Initial email: " + email);
        System.out.println("🔍 getEmailFromPrincipal - Has 'id': " + attributes.containsKey("id"));
        System.out.println("🔍 getEmailFromPrincipal - Has 'login': " + attributes.containsKey("login"));

        // If email is null and we have GitHub attributes, generate fallback email
        if ((email == null || email.isEmpty()) && attributes.containsKey("id") && attributes.containsKey("login")) {
            String login = (String) attributes.get("login");
            email = login + "@users.noreply.github.com";
            System.out.println("🔍 getEmailFromPrincipal - Generated fallback email: " + email);
        }

        System.out.println("🔍 getEmailFromPrincipal - Final email: " + email);
        return email;
    }
}
