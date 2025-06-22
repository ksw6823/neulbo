package io.neulbo.backend.user.service;

import io.neulbo.backend.user.domain.User;
import io.neulbo.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 테스트")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("ADMIN")
                .build();
    }

    @Test
    @DisplayName("성공: 역할 변경 성공")
    void changeUserRole_Success() {
        // given
        Long userId = 1L;
        String newRole = "USER";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateUserRole(userId, "USER")).thenReturn(1);

        // when
        boolean result = userService.changeUserRole(userId, newRole);

        // then
        assertThat(result).isTrue();
        verify(userRepository).findById(userId);
        verify(userRepository).updateUserRole(userId, "USER");
    }

    @Test
    @DisplayName("실패: 이미 동일한 역할인 경우")
    void changeUserRole_SameRole() {
        // given
        Long userId = 1L;
        String newRole = "ADMIN";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // when
        boolean result = userService.changeUserRole(userId, newRole);

        // then
        assertThat(result).isFalse();
        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("성공: 대소문자 무관 역할 비교 - 이미 동일한 역할 (소문자)")
    void changeUserRole_SameRole_CaseInsensitive_Lowercase() {
        // given
        Long userId = 1L;
        User userWithLowercaseRole = User.builder()
                .id(userId)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("admin")  // 소문자로 저장된 경우
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithLowercaseRole));

        // when
        boolean result = userService.changeUserRole(userId, "ADMIN");  // 대문자로 요청

        // then
        assertThat(result).isFalse();  // 이미 동일한 역할이므로 변경되지 않음
        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("성공: 대소문자 무관 역할 비교 - 이미 동일한 역할 (혼합)")
    void changeUserRole_SameRole_CaseInsensitive_MixedCase() {
        // given
        Long userId = 1L;
        User userWithMixedCaseRole = User.builder()
                .id(userId)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("User")  // 혼합 대소문자로 저장된 경우
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(userWithMixedCaseRole));

        // when
        boolean result = userService.changeUserRole(userId, "user");  // 소문자로 요청

        // then
        assertThat(result).isFalse();  // 이미 동일한 역할이므로 변경되지 않음
        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 사용자 ID가 null인 경우")
    void changeUserRole_NullUserId() {
        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(null, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 null일 수 없습니다");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 역할이 null인 경우")
    void changeUserRole_NullRole() {
        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("역할은 null이거나 비어있을 수 없습니다");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 역할이 빈 문자열인 경우")
    void changeUserRole_EmptyRole() {
        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(1L, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("역할은 null이거나 비어있을 수 없습니다");

        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 금지된 역할(ADMIN) 할당 시도")
    void changeUserRole_ForbiddenRole_Admin() {
        // given
        Long userId = 1L;
        User currentUser = User.builder()
                .id(userId)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("USER")  // 현재 USER 역할
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(userId, "ADMIN"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("보안상의 이유로 ADMIN 역할은 할당할 수 없습니다");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 금지된 역할(SUPER_ADMIN) 할당 시도")
    void changeUserRole_ForbiddenRole_SuperAdmin() {
        // given
        Long userId = 1L;
        User currentUser = User.builder()
                .id(userId)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("USER")  // 현재 USER 역할
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(userId, "SUPER_ADMIN"))
                .isInstanceOf(SecurityException.class)
                .hasMessage("보안상의 이유로 SUPER_ADMIN 역할은 할당할 수 없습니다");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 허용되지 않은 역할 할당 시도")
    void changeUserRole_NotAllowedRole() {
        // given
        Long userId = 1L;
        User currentUser = User.builder()
                .id(userId)
                .socialId("test123")
                .provider("google")
                .email("test@example.com")
                .nickname("testUser")
                .role("USER")  // 현재 USER 역할
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(currentUser));

        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(userId, "MANAGER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("허용되지 않은 역할입니다");

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자")
    void changeUserRole_UserNotFound() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(userId, "USER"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다: " + userId);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).updateUserRole(any(), any());
    }

    @Test
    @DisplayName("실패: 데이터베이스 업데이트 실패")
    void changeUserRole_DatabaseUpdateFailed() {
        // given
        Long userId = 1L;
        String newRole = "USER";
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateUserRole(userId, "USER")).thenReturn(0); // 업데이트 실패

        // when & then
        assertThatThrownBy(() -> userService.changeUserRole(userId, newRole))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("역할 변경에 실패했습니다");

        verify(userRepository).findById(userId);
        verify(userRepository).updateUserRole(userId, "USER");
    }

    @Test
    @DisplayName("성공: 대소문자 무관하게 역할 정규화")
    void changeUserRole_CaseInsensitive() {
        // given
        Long userId = 1L;
        String newRole = "user"; // 소문자로 입력
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.updateUserRole(userId, "USER")).thenReturn(1);

        // when
        boolean result = userService.changeUserRole(userId, newRole);

        // then
        assertThat(result).isTrue();
        verify(userRepository).findById(userId);
        verify(userRepository).updateUserRole(userId, "USER"); // 대문자로 정규화됨
    }

    @Test
    @DisplayName("성공: 사용자 조회")
    void findUserById_Success() {
        // given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

        // when
        User result = userService.findUserById(userId);

        // then
        assertThat(result).isEqualTo(testUser);
        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("실패: 사용자 조회 - ID가 null")
    void findUserById_NullId() {
        // when & then
        assertThatThrownBy(() -> userService.findUserById(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자 ID는 null일 수 없습니다");

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("실패: 사용자 조회 - 존재하지 않는 사용자")
    void findUserById_UserNotFound() {
        // given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("사용자를 찾을 수 없습니다: " + userId);

        verify(userRepository).findById(userId);
    }
} 