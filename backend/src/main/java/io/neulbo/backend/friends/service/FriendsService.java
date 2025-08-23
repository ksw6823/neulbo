package io.neulbo.backend.friends.service;

import io.neulbo.backend.friends.domain.Friends;
import io.neulbo.backend.friends.dto.FriendsDTO;
import io.neulbo.backend.friends.repository.FriendsRepository;
import io.neulbo.backend.global.error.ErrorCode;
import io.neulbo.backend.global.exception.BusinessException;
import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendsService {

    private final FriendsRepository friendsRepository;
    private final UserRepository userRepository;

    // 자기자신 x
    public FriendsDTO follow(User fromUser, User toUser, UserDetails userDetails) {
        // 중복 x
        if (friendsRepository.existsByFromUserAndToUser(fromUser, toUser)) {
            throw new BusinessException(ErrorCode.ALREADY_FOLLOW);
        }

        Friends friends = Friends.builder()
            .toUser(toUser)
            .fromUser(fromUser)
            .build();

        Friends savedFriends = friendsRepository.save(friends);

        return new FriendsDTO(savedFriends.getToUser().getUsername());
    }

    public List<FriendsDTO> followingList(User fromUser) {
        return fromUser.getFollowings().stream()
                .map(follow -> new FriendsDTO(
                        follow.getToUser().getUsername()
                ))
                .collect(Collectors.toList());
    }

    public List<FriendsDTO> followerList(User toUser) {
        return toUser.getFollowers().stream()
                .map(follow -> new FriendsDTO(
                        follow.getFromUser().getUsername()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelFollow(User fromUser, User toUser){
        Friends follow = friendsRepository.findByFromUserAndToUser(fromUser, toUser)
                .orElseThrow(() -> new BusinessException(ErrorCode.FOLLOW_NOT_FOUND));
        friendsRepository.delete(follow);
    }
}
