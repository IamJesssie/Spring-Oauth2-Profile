package com.example.springoauth2profile.repository;

import com.example.springoauth2profile.model.AuthProvider;
import com.example.springoauth2profile.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AuthProviderRepository extends JpaRepository<AuthProvider, Long> {
    Optional<AuthProvider> findByProviderAndProviderUserId(Provider provider, String providerUserId);
}
