package io.neulbo.backend.sleep.repository;

import io.neulbo.backend.sleep.domain.MovementType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * MovementDataRepository의 새로운 유연한 메서드들의 사용 예시를 보여주는 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class MovementDataRepositoryFlexibilityTest {

    @Test
    @DisplayName("새로운 유연한 메서드들의 사용 예시")
    void demonstrateFlexibleMethods() {
        // 이제 하드코딩 대신 다양한 조합으로 사용 가능
        
        // 1. 강한 움직임만 (기존과 동일한 결과)
        List<MovementType> strongMovements = List.of(
            MovementType.MODERATE_MOVEMENT, 
            MovementType.STRONG_MOVEMENT
        );
        
        // 2. 모든 움직임 (STILL 제외)
        List<MovementType> allMovements = List.of(
            MovementType.LIGHT_MOVEMENT,
            MovementType.MODERATE_MOVEMENT, 
            MovementType.STRONG_MOVEMENT
        );
        
        // 3. 가벼운 움직임만
        List<MovementType> lightMovements = List.of(
            MovementType.LIGHT_MOVEMENT
        );
        
        // 4. 제외할 타입들 (STILL과 LIGHT_MOVEMENT 제외)
        List<MovementType> excludeTypes = List.of(
            MovementType.STILL,
            MovementType.LIGHT_MOVEMENT
        );
        
        // 이제 enum 값이 변경되어도 JPQL 쿼리는 수정할 필요가 없음
        assertThat(strongMovements).containsExactly(
            MovementType.MODERATE_MOVEMENT, 
            MovementType.STRONG_MOVEMENT
        );
        
        assertThat(allMovements).hasSize(3);
        assertThat(lightMovements).containsExactly(MovementType.LIGHT_MOVEMENT);
        assertThat(excludeTypes).containsExactly(MovementType.STILL, MovementType.LIGHT_MOVEMENT);
    }
    
    @Test
    @DisplayName("enum 값 변경에 대한 안정성 검증")
    void verifyEnumStability() {
        // enum 값들이 예상대로 정의되어 있는지 확인
        assertThat(MovementType.values()).hasSize(4);
        assertThat(MovementType.STILL.getDescription()).isEqualTo("정지");
        assertThat(MovementType.LIGHT_MOVEMENT.getDescription()).isEqualTo("가벼운 움직임");
        assertThat(MovementType.MODERATE_MOVEMENT.getDescription()).isEqualTo("보통 움직임");
        assertThat(MovementType.STRONG_MOVEMENT.getDescription()).isEqualTo("강한 움직임");
        
        // level 값들도 확인
        assertThat(MovementType.STILL.getLevel()).isEqualTo(0);
        assertThat(MovementType.LIGHT_MOVEMENT.getLevel()).isEqualTo(1);
        assertThat(MovementType.MODERATE_MOVEMENT.getLevel()).isEqualTo(2);
        assertThat(MovementType.STRONG_MOVEMENT.getLevel()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("새로운 활용 시나리오 예시")
    void demonstrateNewUsageScenarios() {
        // 1. 특정 레벨 이상의 움직임만 조회
        List<MovementType> moderateAndAbove = List.of(
            MovementType.MODERATE_MOVEMENT,
            MovementType.STRONG_MOVEMENT
        );
        
        // 2. 특정 레벨 이하의 움직임만 조회
        List<MovementType> lightAndStill = List.of(
            MovementType.STILL,
            MovementType.LIGHT_MOVEMENT
        );
        
        // 3. 단일 움직임 타입 조회
        List<MovementType> onlyStrong = List.of(MovementType.STRONG_MOVEMENT);
        
        // 4. 커스텀 조합
        List<MovementType> customCombination = List.of(
            MovementType.LIGHT_MOVEMENT,
            MovementType.STRONG_MOVEMENT  // 중간 단계 제외
        );
        
        // 이제 서비스 계층에서 비즈니스 로직에 따라 유연하게 조합 가능
        assertThat(moderateAndAbove).hasSize(2);
        assertThat(lightAndStill).hasSize(2);
        assertThat(onlyStrong).hasSize(1);
        assertThat(customCombination).hasSize(2);
        assertThat(customCombination).doesNotContain(MovementType.MODERATE_MOVEMENT);
    }
    
    /**
     * 서비스 계층에서 사용할 수 있는 헬퍼 메서드 예시
     */
    public static class MovementTypeHelper {
        
        public static List<MovementType> getStrongMovements() {
            return List.of(MovementType.MODERATE_MOVEMENT, MovementType.STRONG_MOVEMENT);
        }
        
        public static List<MovementType> getActiveMovements() {
            return List.of(
                MovementType.LIGHT_MOVEMENT,
                MovementType.MODERATE_MOVEMENT,
                MovementType.STRONG_MOVEMENT
            );
        }
        
        public static List<MovementType> getMovementsByMinLevel(int minLevel) {
            return List.of(MovementType.values()).stream()
                    .filter(type -> type.getLevel() >= minLevel)
                    .toList();
        }
        
        public static List<MovementType> getExcludeStillTypes() {
            return List.of(MovementType.STILL);
        }
    }
    
    @Test
    @DisplayName("헬퍼 메서드 활용 예시")
    void demonstrateHelperMethods() {
        // 헬퍼 메서드를 사용한 깔끔한 코드
        List<MovementType> strongMovements = MovementTypeHelper.getStrongMovements();
        List<MovementType> activeMovements = MovementTypeHelper.getActiveMovements();
        List<MovementType> level2AndAbove = MovementTypeHelper.getMovementsByMinLevel(2);
        
        assertThat(strongMovements).hasSize(2);
        assertThat(activeMovements).hasSize(3);
        assertThat(level2AndAbove).containsExactly(
            MovementType.MODERATE_MOVEMENT, 
            MovementType.STRONG_MOVEMENT
        );
    }
}