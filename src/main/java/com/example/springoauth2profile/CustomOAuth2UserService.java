package com.example.springoauth2profile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserRepository userRepository;
    private final AuthProviderRepository authProviderRepository;

    public CustomOAuth2UserService(UserRepository userRepository, AuthProviderRepository authProviderRepository) {
        this.userRepository = userRepository;
        this.authProviderRepository = authProviderRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String clientRegistrationId = userRequest.getClientRegistration().getRegistrationId();
        logger.info("=== CustomOAuth2UserService.loadUser() STARTED ===");
        logger.info("Client Registration ID: {}", clientRegistrationId);
        logger.info("Access Token: {}", userRequest.getAccessToken().getTokenValue() != null ? "PRESENT" : "NULL");

        // Enhanced logging for debugging Google vs GitHub
        if ("google".equals(clientRegistrationId)) {
            logger.error("🔍 GOOGLE OAUTH2 LOGIN DETECTED - DEBUGGING THIS CASE");
        } else if ("github".equals(clientRegistrationId)) {
            logger.error("✅ GITHUB OAUTH2 LOGIN DETECTED - This works correctly");
        } else {
            logger.error("❓ UNKNOWN OAUTH2 PROVIDER: {}", clientRegistrationId);
        }

        try {
            logger.info("Calling super.loadUser()...");
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            logger.info("OAuth2User loaded successfully");
            logger.info("Available attributes keys: {}", attributes.keySet());
            logger.info("Full attributes: {}", attributes);

            String providerStr = userRequest.getClientRegistration().getRegistrationId();
            logger.info("Provider string from request: '{}'", providerStr);

            Provider provider = null;
            try {
                provider = Provider.valueOf(providerStr.toUpperCase());
                logger.info("Successfully mapped '{}' to provider enum: {}", providerStr, provider);
            } catch (IllegalArgumentException e) {
                logger.error("❌ FAILED to map provider string '{}' to Provider enum", providerStr, e);
                throw new OAuth2AuthenticationException("Unsupported provider: " + providerStr);
            }

            logger.info("Processing OAuth2 user for provider: {}", provider);

        String providerUserId = null;
        if (provider == Provider.GOOGLE) {
            providerUserId = (String) attributes.get("sub");
            logger.info("🔍 GOOGLE - Extracted providerUserId: '{}' from 'sub' attribute", providerUserId);
        } else if (provider == Provider.GITHUB) {
            providerUserId = String.valueOf(attributes.get("id"));
            logger.info("✅ GITHUB - Extracted providerUserId: '{}' from 'id' attribute", providerUserId);
        }

        if (providerUserId == null) {
            logger.error("❌ FAILED to extract providerUserId for provider: {}", provider);
            logger.error("Available attributes: {}", attributes.keySet());
            logger.error("Sub attribute value: {}", attributes.get("sub"));
            logger.error("ID attribute value: {}", attributes.get("id"));
            throw new OAuth2AuthenticationException("Could not find provider user ID");
        }

        logger.info("🔍 Checking if AuthProvider exists for provider: {}, providerUserId: {}", provider, providerUserId);
        Optional<AuthProvider> authProviderOpt = authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);
        logger.info("AuthProvider lookup result - Present: {}", authProviderOpt.isPresent());

        User user;
        if (authProviderOpt.isPresent()) {
            user = authProviderOpt.get().getUser();
            logger.info("✅ Found existing user via AuthProvider: {}", user.getEmail());
        } else {
            logger.info("❌ No existing AuthProvider found - will create new user");
            String email = (String) attributes.get("email");
            if (email == null) {
                
                if (provider == Provider.GITHUB) {
                    String login = (String) attributes.get("login");
                    email = login + "@users.noreply.github.com";
                } else {
                    throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
                }
            }
            
            String displayName = (String) attributes.get("name");
            String avatarUrl = (provider == Provider.GITHUB) ? (String) attributes.get("avatar_url") : (String) attributes.get("picture");

            Optional<User> userOpt = userRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                user = new User();
                user.setEmail(email);
                user.setDisplayName(displayName);
                user.setAvatarUrl(avatarUrl);
            }

            logger.info("🔄 Creating new AuthProvider for provider: {}, providerUserId: {}", provider, providerUserId);
            AuthProvider newAuthProvider = new AuthProvider();
            newAuthProvider.setProvider(provider);
            newAuthProvider.setProviderUserId(providerUserId);
            newAuthProvider.setUser(user);

            logger.info("💾 Saving User to database: {}", user.getEmail());
            try {
                userRepository.save(user);
                logger.info("✅ User saved successfully");
            } catch (Exception e) {
                logger.error("❌ FAILED to save User: {}", user.getEmail(), e);
                throw e;
            }

            logger.info("💾 Saving AuthProvider to database");
            try {
                authProviderRepository.save(newAuthProvider);
                logger.info("✅ AuthProvider saved successfully");
            } catch (Exception e) {
                logger.error("❌ FAILED to save AuthProvider", e);
                throw e;
            }
        }

        logger.debug("Successfully processed OAuth2 user: {}", user.getEmail());

        String nameAttributeKey = "email";
        if (provider == Provider.GITHUB) {
            nameAttributeKey = "login";
        }

        java.util.Set<org.springframework.security.core.GrantedAuthority> authorities = new java.util.HashSet<>();
        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"));

        org.springframework.security.oauth2.core.user.OAuth2User userDetails = new org.springframework.security.oauth2.core.user.DefaultOAuth2User(authorities, oAuth2User.getAttributes(), nameAttributeKey);

        return userDetails;
    } catch (Exception e) {
        logger.error("Error processing OAuth2 user", e);
        throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + e.getMessage());
    }
}

    /**
     * Process OAuth2User (including OIDC users) and save to database
     */
    public void processOAuth2User(org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest userRequest, 
                                  org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) throws OAuth2AuthenticationException {
        try {
            logger.error("🔍 PROCESSING OAUTH2/OIDC USER - Client Registration: {}", userRequest.getClientRegistration().getRegistrationId());
            
            Map<String, Object> attributes = oAuth2User.getAttributes();
            logger.error("User attributes: {}", attributes);

            String providerStr = userRequest.getClientRegistration().getRegistrationId();
            Provider provider = Provider.valueOf(providerStr.toUpperCase());
            
            String providerUserId = null;
            String email = null;
            String displayName = null;
            String avatarUrl = null;
            
            if (provider == Provider.GOOGLE) {
                providerUserId = (String) attributes.get("sub");
                email = (String) attributes.get("email");
                displayName = (String) attributes.get("name");
                avatarUrl = (String) attributes.get("picture");
                logger.error("🔍 GOOGLE - providerUserId: {}, email: {}, name: {}", providerUserId, email, displayName);
            } else if (provider == Provider.GITHUB) {
                providerUserId = String.valueOf(attributes.get("id"));
                email = (String) attributes.get("email");
                if (email == null) {
                    String login = (String) attributes.get("login");
                    email = login + "@users.noreply.github.com";
                }
                displayName = (String) attributes.get("name");
                avatarUrl = (String) attributes.get("avatar_url");
                logger.error("✅ GITHUB - providerUserId: {}, email: {}, name: {}", providerUserId, email, displayName);
            }

            if (providerUserId == null) {
                throw new OAuth2AuthenticationException("Could not find provider user ID");
            }

            // Check if AuthProvider exists
            Optional<AuthProvider> authProviderOpt = authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);

            User user;
            if (authProviderOpt.isPresent()) {
                user = authProviderOpt.get().getUser();
                logger.error("✅ Found existing user: {}", user.getEmail());
            } else {
                // Check if user exists by email
                Optional<User> userOpt = userRepository.findByEmail(email);

                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    logger.error("✅ Found existing user by email: {}", user.getEmail());
                } else {
                    user = new User();
                    user.setEmail(email);
                    user.setDisplayName(displayName);
                    user.setAvatarUrl(avatarUrl);
                    logger.error("🔄 Creating new user: {}", email);
                }

                // Create new AuthProvider
                AuthProvider newAuthProvider = new AuthProvider();
                newAuthProvider.setProvider(provider);
                newAuthProvider.setProviderUserId(providerUserId);
                newAuthProvider.setUser(user);

                logger.error("💾 Saving User and AuthProvider to database");
                userRepository.save(user);
                authProviderRepository.save(newAuthProvider);
                logger.error("✅ Successfully saved user and auth provider");
            }

        } catch (Exception e) {
            logger.error("❌ Error processing OAuth2/OIDC user", e);
            throw new OAuth2AuthenticationException("Error processing OAuth2/OIDC user: " + e.getMessage());
        }
    }
}
