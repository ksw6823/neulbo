package io.neulbo.backend.friends.repository;

import io.neulbo.backend.friends.domain.Friends;
import io.neulbo.backend.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FriendsRepository extends JpaRepository<Friends, Long> {

    List<Friends> findByFromUser(User fromuser);
    List<Friends> findByToUser(User touser);
    void  deleteFollowByFromUser(User fromuser);
    @Query("SELECT f FROM Friends f WHERE f.fromUser = :fromUser AND f.toUser = :toUser")
    Optional<Friends> findByFromUserAndToUser(User fromUser, User toUser);
    boolean existsByFromUserAndToUser(User fromUser, User toUser);
}