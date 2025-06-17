package io.neulbo.backend.auth.util;

import io.neulbo.backend.auth.dto.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 현재 인증된 사용자 정보에 접근하는 유틸리티 클래스
 */
public class SecurityUtils {

    /**
     * 현재 인증된 사용자의 ID를 반환합니다.
     * 
     * @return 현재 사용자의 ID, 인증되지 않은 경우 null
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            return userDetails.getUserId();
        }
        return null;
    }

    /**
     * 현재 인증된 사용자의 CustomUserDetails를 반환합니다.
     * 
     * @return 현재 사용자의 CustomUserDetails, 인증되지 않은 경우 null
     */
    public static CustomUserDetails getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return (CustomUserDetails) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 현재 사용자가 인증되었는지 확인합니다.
     * 
     * @return 인증된 경우 true, 그렇지 않으면 false
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && 
               authentication.isAuthenticated() && 
               authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * 현재 사용자의 OAuth 제공자를 반환합니다.
     * 
     * @return OAuth 제공자명, 인증되지 않은 경우 null
     */
    public static String getCurrentUserProvider() {
        CustomUserDetails userDetails = getCurrentUserDetails();
        return userDetails != null ? userDetails.getProvider() : null;
    }
} 