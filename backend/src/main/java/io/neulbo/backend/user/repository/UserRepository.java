package io.neulbo.backend.user.repository;


import io.neulbo.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndProvider(String socialId, String provider);

    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.role = :role WHERE u.id = :userId")
    int updateUserRole(@Param("userId") Long userId, @Param("role") String role);

//    필요하다면 사용
//    Optional<User> findByEmailAndProvider(String email, String provider);
//    Optional<User> findByNicknameAndProvider(String nickname, String provider);
}