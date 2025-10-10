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
        try {
            OAuth2User oAuth2User = super.loadUser(userRequest);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String providerStr = userRequest.getClientRegistration().getRegistrationId();
            Provider provider = Provider.valueOf(providerStr.toUpperCase());
            
            logger.debug("Processing OAuth2 user for provider: {}", provider);
            logger.debug("User attributes: {}", attributes);

        String providerUserId = null;
        if (provider == Provider.GOOGLE) {
            providerUserId = (String) attributes.get("sub");
        } else if (provider == Provider.GITHUB) {
            providerUserId = String.valueOf(attributes.get("id"));
        }

        if (providerUserId == null) {
            throw new OAuth2AuthenticationException("Could not find provider user ID");
        }

        Optional<AuthProvider> authProviderOpt = authProviderRepository.findByProviderAndProviderUserId(provider, providerUserId);

        User user;
        if (authProviderOpt.isPresent()) {
            user = authProviderOpt.get().getUser();
        } else {
            String email = (String) attributes.get("email");
            if (email == null) {
                // For GitHub users with private emails, use a fallback email
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

            AuthProvider newAuthProvider = new AuthProvider();
            newAuthProvider.setProvider(provider);
            newAuthProvider.setProviderUserId(providerUserId);
            newAuthProvider.setUser(user);
            
            userRepository.save(user);
            authProviderRepository.save(newAuthProvider);
        }

        logger.debug("Successfully processed OAuth2 user: {}", user.getEmail());
        
        // Note: In a real app, you'd return a custom AppUser principal object
        // that wraps your User entity, not the default oAuth2User.
        // For this project, returning the default is sufficient for now.
        return oAuth2User;
        } catch (Exception e) {
            logger.error("Error processing OAuth2 user", e);
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + e.getMessage());
        }
    }
}
