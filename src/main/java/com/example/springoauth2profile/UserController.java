package com.example.springoauth2profile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @GetMapping("/profile")
    public OAuth2User profile(@AuthenticationPrincipal OAuth2User principal) {
        // For now, just return the principal object to see the attributes.
        // In the next milestone, we will map this to our User entity.
        return principal;
    }
}
