package com.game3alhaq.service;

import com.game3alhaq.dto.request.MediaRequests.CreateVideoRequest;
import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.entity.Video;
import com.game3alhaq.mapper.VideoMapper;
import com.game3alhaq.repository.VideoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/|youtube\\.com\\/shorts\\/)([^\"&?\\/\\s]{11})",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Seeds initial video if database table is currently empty
     */
    @PostConstruct
    public void initDefaultVideo() {
        if (videoRepository.count() == 0) {
            Video defaultVideo = Video.builder()
                    .youtubeUrl("https://youtu.be/dkeu6gi3mAc?si=yUSf8JfNVktQWeYD")
                    .youtubeId("dkeu6gi3mAc")
                    .title("السبت الممتاز | نجم الجيل | مع ويجز | THE GOATS")
                    .description("لقاء ممتع وحصري من برنامج السبت الممتاز مع النجم تامر حسني وويجز، متاح للمشاهدة مباشرة عبر المشغل المدمج في الموقع بجودة عالية.")
                    .category("lessons")
                    .categoryName("حوارات وبرامج")
                    .duration("45:02")
                    .viewsCount("2,027,645")
                    .isFeatured(true)
                    .build();

            videoRepository.save(defaultVideo);
            log.info("Initialized default YouTube video: {}", defaultVideo.getYoutubeId());
        }
    }

    /**
     * Extracts the 11-character YouTube video ID
     */
    public String extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.matches("^[a-zA-Z0-9_-]{11}$")) {
            return trimmed;
        }
        Matcher matcher = YOUTUBE_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Fetches video title via YouTube oEmbed API if title was not provided
     */
    public String fetchYouTubeTitleOEmbed(String url) {
        try {
            String oembedUrl = "https://www.youtube.com/oembed?url=" + url + "&format=json";
            ResponseEntity<Map> response = restTemplate.getForEntity(oembedUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object title = response.getBody().get("title");
                if (title != null) {
                    return title.toString();
                }
            }
        } catch (Exception ex) {
            log.warn("Could not fetch YouTube title via oEmbed for {}: {}", url, ex.getMessage());
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<VideoResponse> getAllVideos() {
        return videoRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(videoMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VideoResponse getVideoById(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("الفيديو غير موجود برقم المعرف: " + id));
        return videoMapper.toResponse(video);
    }

    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        String youtubeId = extractYouTubeId(request.getYoutubeUrl());
        if (youtubeId == null) {
            throw new IllegalArgumentException("رابط يوتيوب غير صالح. يرجى التأكد من الرابط.");
        }

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            title = fetchYouTubeTitleOEmbed(request.getYoutubeUrl());
            if (title == null || title.trim().isEmpty()) {
                title = "فيديو يوتيوب (" + youtubeId + ")";
            }
        }

        String category = request.getCategory() != null && !request.getCategory().isBlank()
                ? request.getCategory()
                : "lessons";

        String categoryName = request.getCategoryName() != null && !request.getCategoryName().isBlank()
                ? request.getCategoryName()
                : "دروس ومواعظ";

        boolean isFeatured = Boolean.TRUE.equals(request.getIsFeatured());
        if (isFeatured) {
            // Un-feature previous videos
            videoRepository.findAll().forEach(v -> {
                if (Boolean.TRUE.equals(v.getIsFeatured())) {
                    v.setIsFeatured(false);
                    videoRepository.save(v);
                }
            });
        }

        Video video = Video.builder()
                .youtubeUrl(request.getYoutubeUrl().trim())
                .youtubeId(youtubeId)
                .title(title.trim())
                .description(request.getDescription())
                .category(category)
                .categoryName(categoryName)
                .duration(request.getDuration() != null ? request.getDuration() : "غير محدد")
                .viewsCount(request.getViewsCount() != null ? request.getViewsCount() : "جديد")
                .customPoster(request.getCustomPoster())
                .isFeatured(isFeatured)
                .build();

        Video saved = videoRepository.save(video);
        return videoMapper.toResponse(saved);
    }

    @Transactional
    public void deleteVideo(Long id) {
        if (!videoRepository.existsById(id)) {
            throw new IllegalArgumentException("الفيديو غير موجود برقم المعرف: " + id);
        }
        videoRepository.deleteById(id);
    }
}
