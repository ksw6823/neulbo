package io.neulbo.backend.user.repository;

import io.neulbo.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByProviderIdAndProvider(String providerId, String provider);
    Optional<User> findByUsername(String username);

    // 필요하다면 사용
    // Optional<User> findByUsernameAndProvider(String username, String provider);
}