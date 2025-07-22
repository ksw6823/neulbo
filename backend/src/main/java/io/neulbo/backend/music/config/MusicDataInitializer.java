package io.neulbo.backend.music.config;

import io.neulbo.backend.music.domain.Category;
import io.neulbo.backend.music.domain.Music;
import io.neulbo.backend.music.repository.CategoryRepository;
import io.neulbo.backend.music.repository.MusicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 음악 데이터 초기화 컴포넌트
 * 
 * 주의: 현재 모든 URL들(iconUrl, fileUrl, thumbnailUrl)은 개발용 임시 데이터입니다.
 * 프로덕션 배포 전에 실제 파일 경로 또는 CDN URL로 교체해야 합니다.
 * 
 * TODO: 
 * - 실제 음악 파일 준비 및 업로드
 * - 카테고리 아이콘 파일 준비 및 업로드  
 * - 음악 썸네일 이미지 준비 및 업로드
 * - application.yml에서 baseUrl 설정 후 동적 URL 생성으로 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MusicDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final MusicRepository musicRepository;

    @Override
    public void run(String... args) throws Exception {
        // 카테고리 초기화
        if (categoryRepository.count() == 0) {
            try {
                initializeCategories();
                log.info("음악 카테고리 초기화 완료");
            } catch (Exception e) {
                log.error("음악 카테고리 초기화 실패: {}", e.getMessage(), e);
                log.warn("카테고리 초기화 실패로 인해 일부 기능이 제한될 수 있습니다.");
            }
        }
        
        // 샘플 음악 초기화
        if (musicRepository.count() == 0) {
            try {
                initializeSampleMusic();
                log.info("샘플 음악 초기화 완료");
            } catch (Exception e) {
                log.error("샘플 음악 초기화 실패: {}", e.getMessage(), e);
                log.warn("샘플 음악 초기화 실패로 인해 일부 기능이 제한될 수 있습니다.");
            }
        }
    }

    private void initializeCategories() {
        log.info("음악 카테고리 초기 데이터 생성 시작...");

        // TODO: 실제 음악 파일 및 아이콘 준비 후 URL 업데이트 필요
        // 현재 iconUrl은 개발용 임시 데이터로, 프로덕션 환경에서는 실제 파일 경로로 변경해야 함
        List<Category> categories = List.of(
            Category.builder()
                .name("수면 음악")
                .description("깊은 잠에 들 수 있도록 도와주는 부드러운 음악")
                .iconUrl("https://example.com/icons/sleep.png") // 임시 URL - 실제 아이콘 파일로 교체 필요
                .colorCode("#2E3A59")
                .sortOrder(1)
                .isActive(true)
                .build(),

            Category.builder()
                .name("명상 음악")
                .description("마음의 평안과 집중력을 높여주는 명상 음악")
                .iconUrl("https://example.com/icons/meditation.png")
                .colorCode("#4A5C6A")
                .sortOrder(2)
                .isActive(true)
                .build(),

            Category.builder()
                .name("자연음")
                .description("비, 바다, 숲소리 등 자연의 소리")
                .iconUrl("https://example.com/icons/nature.png")
                .colorCode("#2D5A27")
                .sortOrder(3)
                .isActive(true)
                .build(),

            Category.builder()
                .name("백색소음")
                .description("일정한 주파수의 배경음으로 집중력 향상")
                .iconUrl("https://example.com/icons/whitenoise.png")
                .colorCode("#6C757D")
                .sortOrder(4)
                .isActive(true)
                .build(),

            Category.builder()
                .name("바이노럴 비트")
                .description("뇌파 동조를 통한 수면 및 집중 유도")
                .iconUrl("https://example.com/icons/binaural.png")
                .colorCode("#5A4FCF")
                .sortOrder(5)
                .isActive(true)
                .build(),

            Category.builder()
                .name("피아노")
                .description("부드럽고 따뜻한 피아노 연주곡")
                .iconUrl("https://example.com/icons/piano.png")
                .colorCode("#8B4513")
                .sortOrder(6)
                .isActive(true)
                .build()
        );

        categoryRepository.saveAll(categories);
        log.info("카테고리 {} 개 생성 완료", categories.size());
    }

    private void initializeSampleMusic() {
        log.info("샘플 음악 데이터 생성 시작...");

        // TODO: 실제 음악 파일 및 썸네일 준비 후 URL 업데이트 필요
        // 현재 fileUrl, thumbnailUrl은 개발용 임시 데이터로, 프로덕션 환경에서는 실제 파일 경로로 변경해야 함
        List<Category> categories = categoryRepository.findAll();
        
        if (categories.isEmpty()) {
            log.warn("카테고리가 없어서 샘플 음악을 생성할 수 없습니다");
            return;
        }

        // 카테고리별 샘플 음악 데이터
        Category sleepCategory = categories.stream()
                .filter(c -> "수면 음악".equals(c.getName()))
                .findFirst()
                .orElse(categories.get(0));

        Category meditationCategory = categories.stream()
                .filter(c -> "명상 음악".equals(c.getName()))
                .findFirst()
                .orElse(categories.get(0));

        Category natureCategory = categories.stream()
                .filter(c -> "자연음".equals(c.getName()))
                .findFirst()
                .orElse(categories.get(0));

        List<Music> sampleMusic = List.of(
            // 수면 음악
            Music.builder()
                .title("Peaceful Sleep")
                .artist("Sleep Studio")
                .album("Deep Rest")
                .durationSeconds(3600) // 60분
                .fileUrl("https://example.com/music/peaceful-sleep.mp3") // 임시 URL - 실제 음악 파일로 교체 필요
                .thumbnailUrl("https://example.com/thumbnails/peaceful-sleep.jpg") // 임시 URL - 실제 썸네일로 교체 필요
                .description("깊은 잠에 들 수 있도록 도와주는 부드러운 멜로디")
                .isPremium(false)
                .isActive(true)
                .category(sleepCategory)
                .build(),

            Music.builder()
                .title("Dream Waves")
                .artist("Relaxation Masters")
                .album("Sleep Collection")
                .durationSeconds(2700) // 45분
                .fileUrl("https://example.com/music/dream-waves.mp3")
                .thumbnailUrl("https://example.com/thumbnails/dream-waves.jpg")
                .description("꿈속으로 떠나는 듯한 파도 소리")
                .isPremium(true)
                .isActive(true)
                .category(sleepCategory)
                .build(),

            // 명상 음악
            Music.builder()
                .title("Mindful Moments")
                .artist("Zen Collective")
                .album("Meditation Series")
                .durationSeconds(1800) // 30분
                .fileUrl("https://example.com/music/mindful-moments.mp3")
                .thumbnailUrl("https://example.com/thumbnails/mindful-moments.jpg")
                .description("마음을 평온하게 만드는 명상 음악")
                .isPremium(false)
                .isActive(true)
                .category(meditationCategory)
                .build(),

            Music.builder()
                .title("Inner Peace")
                .artist("Meditation Space")
                .album("Calm Mind")
                .durationSeconds(2400) // 40분
                .fileUrl("https://example.com/music/inner-peace.mp3")
                .thumbnailUrl("https://example.com/thumbnails/inner-peace.jpg")
                .description("내면의 평화를 찾는 명상 여행")
                .isPremium(false)
                .isActive(true)
                .category(meditationCategory)
                .build(),

            // 자연음
            Music.builder()
                .title("Forest Rain")
                .artist("Nature Sounds")
                .album("Natural Ambience")
                .durationSeconds(3600) // 60분
                .fileUrl("https://example.com/music/forest-rain.mp3")
                .thumbnailUrl("https://example.com/thumbnails/forest-rain.jpg")
                .description("숲속에 내리는 빗소리")
                .isPremium(false)
                .isActive(true)
                .category(natureCategory)
                .build(),

            Music.builder()
                .title("Ocean Waves")
                .artist("Seaside Audio")
                .album("Coastal Sounds")
                .durationSeconds(4200) // 70분
                .fileUrl("https://example.com/music/ocean-waves.mp3")
                .thumbnailUrl("https://example.com/thumbnails/ocean-waves.jpg")
                .description("해변의 파도 소리")
                .isPremium(true)
                .isActive(true)
                .category(natureCategory)
                .build()
        );

        musicRepository.saveAll(sampleMusic);
        log.info("샘플 음악 {} 개 생성 완료", sampleMusic.size());
    }
} 