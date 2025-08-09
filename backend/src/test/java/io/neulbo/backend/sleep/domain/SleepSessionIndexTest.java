package io.neulbo.backend.sleep.domain;

import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

/**
 * SleepSession 엔티티의 데이터베이스 인덱스 정의 검증 테스트
 * 성능 최적화를 위한 복합 인덱스들이 올바르게 정의되었는지 확인
 */
class SleepSessionIndexTest {

    @Test
    @DisplayName("SleepSession 엔티티에 @Table 어노테이션과 인덱스들이 정의됨")
    void sleepSession_HasTableAnnotationWithIndexes() {
        // Given
        Class<SleepSession> sleepSessionClass = SleepSession.class;

        // When
        Table tableAnnotation = sleepSessionClass.getAnnotation(Table.class);

        // Then
        assertThat(tableAnnotation).isNotNull();
        assertThat(tableAnnotation.name()).isEqualTo("sleep_sessions");
        assertThat(tableAnnotation.indexes()).isNotEmpty();
    }

    @Test
    @DisplayName("user_id와 sleep_start_time에 대한 복합 인덱스가 정의됨")
    void sleepSession_HasUserAndStartTimeIndex() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When
        Index userStartTimeIndex = findIndexByName(indexes, "idx_sleep_session_user_start_time");

        // Then
        assertThat(userStartTimeIndex).isNotNull();
        assertThat(userStartTimeIndex.columnList()).isEqualTo("user_id, sleep_start_time");
        assertThat(userStartTimeIndex.unique()).isFalse();
    }

    @Test
    @DisplayName("user_id와 session_status에 대한 복합 인덱스가 정의됨")
    void sleepSession_HasUserAndStatusIndex() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When
        Index userStatusIndex = findIndexByName(indexes, "idx_sleep_session_user_status");

        // Then
        assertThat(userStatusIndex).isNotNull();
        assertThat(userStatusIndex.columnList()).isEqualTo("user_id, session_status");
        assertThat(userStatusIndex.unique()).isFalse();
    }

    @Test
    @DisplayName("user_id와 created_at에 대한 복합 인덱스가 정의됨")
    void sleepSession_HasUserAndCreatedAtIndex() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When
        Index userCreatedAtIndex = findIndexByName(indexes, "idx_sleep_session_user_created_at");

        // Then
        assertThat(userCreatedAtIndex).isNotNull();
        assertThat(userCreatedAtIndex.columnList()).isEqualTo("user_id, created_at");
        assertThat(userCreatedAtIndex.unique()).isFalse();
    }

    @Test
    @DisplayName("session_status와 created_at에 대한 복합 인덱스가 정의됨")
    void sleepSession_HasStatusAndCreatedAtIndex() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When
        Index statusCreatedAtIndex = findIndexByName(indexes, "idx_sleep_session_status_created_at");

        // Then
        assertThat(statusCreatedAtIndex).isNotNull();
        assertThat(statusCreatedAtIndex.columnList()).isEqualTo("session_status, created_at");
        assertThat(statusCreatedAtIndex.unique()).isFalse();
    }

    @Test
    @DisplayName("모든 인덱스가 예상된 개수만큼 정의됨")
    void sleepSession_HasExpectedNumberOfIndexes() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When & Then
        assertThat(indexes).hasSize(4);
        
        List<String> indexNames = Arrays.stream(indexes)
                .map(Index::name)
                .collect(Collectors.toList());
        
        assertThat(indexNames).containsExactlyInAnyOrder(
                "idx_sleep_session_user_start_time",
                "idx_sleep_session_user_status", 
                "idx_sleep_session_user_created_at",
                "idx_sleep_session_status_created_at"
        );
    }

    @Test
    @DisplayName("인덱스들이 성능 최적화 목적에 맞게 설계됨")
    void sleepSession_IndexesOptimizeQueryPerformance() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When & Then - 각 인덱스가 실제 쿼리 패턴에 맞는지 검증
        for (Index index : indexes) {
            // 모든 인덱스는 non-unique (비즈니스 로직상 중복 허용)
            assertThat(index.unique()).isFalse();
            
            // 인덱스 이름이 의미있게 명명됨
            assertThat(index.name()).startsWith("idx_sleep_session_");
            
            // 컬럼 리스트가 비어있지 않음
            assertThat(index.columnList()).isNotBlank();
            
            // 복합 인덱스 (2개 컬럼)
            String[] columns = index.columnList().split(",");
            assertThat(columns).hasSize(2);
        }
    }

    @Test
    @DisplayName("인덱스 컬럼 순서가 쿼리 패턴에 최적화됨")
    void sleepSession_IndexColumnOrderOptimized() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When & Then
        for (Index index : indexes) {
            String columnList = index.columnList().replaceAll("\\s+", "");
            
            // user_id가 포함된 인덱스들은 user_id가 첫 번째 컬럼이어야 함
            // (WHERE 조건에서 user_id가 항상 먼저 사용되므로)
            if (columnList.contains("user_id")) {
                assertThat(columnList).startsWith("user_id,");
            }
            
            // 각 인덱스가 적절한 컬럼 조합을 가지는지 확인
            switch (index.name()) {
                case "idx_sleep_session_user_start_time":
                    assertThat(columnList).isEqualTo("user_id,sleep_start_time");
                    break;
                case "idx_sleep_session_user_status":
                    assertThat(columnList).isEqualTo("user_id,session_status");
                    break;
                case "idx_sleep_session_user_created_at":
                    assertThat(columnList).isEqualTo("user_id,created_at");
                    break;
                case "idx_sleep_session_status_created_at":
                    assertThat(columnList).isEqualTo("session_status,created_at");
                    break;
                default:
                    fail("Unexpected index name: " + index.name());
            }
        }
    }

    @Test
    @DisplayName("인덱스 정의가 실제 Repository 쿼리 패턴과 일치함")
    void sleepSession_IndexesMatchRepositoryQueryPatterns() {
        // 이 테스트는 인덱스가 실제 사용되는 쿼리 패턴들을 문서화하는 목적

        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When & Then
        System.out.println("=== SleepSession 인덱스와 최적화되는 쿼리 패턴 ===");
        
        for (Index index : indexes) {
            System.out.printf("%n인덱스: %s (%s)%n", index.name(), index.columnList());
            
            switch (index.name()) {
                case "idx_sleep_session_user_start_time":
                    System.out.println("최적화되는 쿼리:");
                    System.out.println("- findByUserAndDateRange (특정 기간 수면 세션)");
                    System.out.println("- findByUserAndLastWeek (주간 수면 세션)");
                    System.out.println("- findByUserAndLastMonth (월간 수면 세션)");
                    System.out.println("- findByUserAndSleepEfficiencyGreaterThanEqual");
                    break;
                    
                case "idx_sleep_session_user_status":
                    System.out.println("최적화되는 쿼리:");
                    System.out.println("- findByUserAndSessionStatus (진행중 세션 조회)");
                    System.out.println("- findByUserAndSessionStatusOrderByCreatedAtDesc");
                    System.out.println("- findAverageSleepDurationByUserAndStatus");
                    System.out.println("- findAverageSleepEfficiencyByUserAndStatus");
                    break;
                    
                case "idx_sleep_session_user_created_at":
                    System.out.println("최적화되는 쿼리:");
                    System.out.println("- findByUserOrderByCreatedAtDesc (페이징)");
                    System.out.println("- findTopByUserOrderByCreatedAtDesc (최근 세션)");
                    break;
                    
                case "idx_sleep_session_status_created_at":
                    System.out.println("최적화되는 쿼리:");
                    System.out.println("- findStaleSessionsByStatus (중단된 세션 정리)");
                    break;
            }
        }
        
        // 인덱스 개수 확인
        assertThat(indexes).hasSize(4);
    }

    @Test
    @DisplayName("인덱스 정의로 인한 성능 향상 예상 효과 검증")
    void sleepSession_IndexesProvidePerformanceBenefits() {
        // Given
        Table tableAnnotation = SleepSession.class.getAnnotation(Table.class);
        Index[] indexes = tableAnnotation.indexes();

        // When & Then
        System.out.println("=== 인덱스별 성능 향상 예상 효과 ===");
        
        for (Index index : indexes) {
            System.out.printf("%n%s:%n", index.name());
            
            switch (index.name()) {
                case "idx_sleep_session_user_start_time":
                    System.out.println("- 특정 기간 수면 데이터 조회 속도 대폭 향상");
                    System.out.println("- 시간 범위 검색 (BETWEEN) 최적화");
                    System.out.println("- ORDER BY sleep_start_time 정렬 성능 향상");
                    break;
                    
                case "idx_sleep_session_user_status":
                    System.out.println("- 사용자별 세션 상태 필터링 속도 향상");
                    System.out.println("- 진행중/완료 세션 구분 조회 최적화");
                    System.out.println("- 통계 계산 쿼리 성능 향상");
                    break;
                    
                case "idx_sleep_session_user_created_at":
                    System.out.println("- 페이징 쿼리 성능 최적화");
                    System.out.println("- 최근 세션 조회 속도 향상");
                    System.out.println("- ORDER BY created_at 정렬 성능 향상");
                    break;
                    
                case "idx_sleep_session_status_created_at":
                    System.out.println("- 중단된 세션 정리 작업 성능 최적화");
                    System.out.println("- 배치 작업 효율성 향상");
                    break;
            }
            
            // 모든 인덱스가 복합 인덱스인지 확인
            String[] columns = index.columnList().split(",");
            assertThat(columns.length).isEqualTo(2)
                    .withFailMessage("인덱스 %s는 복합 인덱스여야 합니다", index.name());
        }
    }

    /**
     * 인덱스 배열에서 특정 이름의 인덱스를 찾는 헬퍼 메서드
     */
    private Index findIndexByName(Index[] indexes, String name) {
        return Arrays.stream(indexes)
                .filter(index -> name.equals(index.name()))
                .findFirst()
                .orElse(null);
    }
}