package io.neulbo.backend.user.repository;


import io.neulbo.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySocialIdAndProvider(String socialId, String provider);

//    필요하다면 사용
//    Optional<User> findByEmailAndProvider(String email, String provider);
//    Optional<User> findByNicknameAndProvider(String nickname, String provider);
}