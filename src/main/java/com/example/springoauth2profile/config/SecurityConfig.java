package com.example.springoauth2profile.config;

import com.example.springoauth2profile.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    private final CustomOAuth2UserService customOAuth2UserService;

    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
        logger.info("SecurityConfig initialized with CustomOAuth2UserService: {}", customOAuth2UserService.getClass().getSimpleName());
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring SecurityFilterChain with CustomOAuth2UserService");

        http
  
                .authorizeHttpRequests(authorize -> {
                    logger.info("Configuring authorization rules");
                    authorize
                            .requestMatchers("/", "/error", "/webjars/**", "/h2-console/**", "/debug/**", "/test-oauth").permitAll()
                            .anyRequest().authenticated();
                })
                .oauth2Login(oauth2 -> {
                    logger.info("Configuring OAuth2 login with custom user service");
                    oauth2
                            .userInfoEndpoint(userInfo -> {
                                logger.info("Setting custom OAuth2UserService and OidcUserService");
                                userInfo.userService(customOAuth2UserService);
                                userInfo.oidcUserService(customOidcUserService());
                            })
                            .defaultSuccessUrl("/profile/view", true);
                })
                .logout(logout -> {
                    logger.info("Configuring logout functionality");
                    logout
                            .logoutUrl("/logout")
                            .logoutSuccessUrl("/")
                            .invalidateHttpSession(true)
                            .clearAuthentication(true)
                            .deleteCookies("JSESSIONID");
                })
                .csrf(csrf -> {
                    logger.info("Configuring CSRF settings");
                    csrf.ignoringRequestMatchers("/h2-console/**");
                })
                .headers(headers -> {
                    logger.info("Configuring headers for H2 console - disabling frame options");
                    headers.frameOptions(frameOptions -> frameOptions.disable());
                });

        logger.info("SecurityFilterChain configuration completed");
        return http.build();
    }

    @Bean
    public OidcUserService customOidcUserService() {
        return new OidcUserService() {
            @Override
            public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
                logger.info("🔍 OIDC User Service - Processing Google user");
                
                // Load the OIDC user first
                OidcUser oidcUser = super.loadUser(userRequest);
                
                // Create a fake OAuth2UserRequest to process through our custom service
                OAuth2UserRequest oauth2UserRequest = new OAuth2UserRequest(
                    userRequest.getClientRegistration(),
                    userRequest.getAccessToken()
                );
                
                // Process the user data through our custom service
                try {
                    customOAuth2UserService.processOAuth2User(oauth2UserRequest, oidcUser);
                    logger.info("✅ Successfully processed OIDC user through CustomOAuth2UserService");
                } catch (Exception e) {
                    logger.error("❌ Failed to process OIDC user", e);
                }
                
                return oidcUser;
            }
        };
    }
}
