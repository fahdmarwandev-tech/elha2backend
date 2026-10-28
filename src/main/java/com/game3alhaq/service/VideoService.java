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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
                    .videoSourceType("YOUTUBE")
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

    public static class YouTubeMetadata {
        public String title;
        public String duration;
        public String viewsCount;
    }

    /**
     * Automatically extracts YouTube video metadata: exact duration, view count, and title
     */
    public YouTubeMetadata fetchYouTubeMetadata(String videoId) {
        YouTubeMetadata meta = new YouTubeMetadata();
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://www.youtube.com/watch?v=" + videoId))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(java.time.Duration.ofSeconds(8))
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // 1. Extract Duration (approxDurationMs or lengthSeconds)
            Matcher durMatcher = Pattern.compile("\"approxDurationMs\":\"(\\d+)\"").matcher(body);
            if (durMatcher.find()) {
                long ms = Long.parseLong(durMatcher.group(1));
                long totalSeconds = ms / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                if (hours > 0) {
                    meta.duration = String.format("%d:%02d:%02d", hours, minutes, seconds);
                } else {
                    meta.duration = String.format("%02d:%02d", minutes, seconds);
                }
            } else {
                Matcher secMatcher = Pattern.compile("\"lengthSeconds\":\"(\\d+)\"").matcher(body);
                if (secMatcher.find()) {
                    long totalSeconds = Long.parseLong(secMatcher.group(1));
                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;
                    if (hours > 0) {
                        meta.duration = String.format("%d:%02d:%02d", hours, minutes, seconds);
                    } else {
                        meta.duration = String.format("%02d:%02d", minutes, seconds);
                    }
                }
            }

            // 2. Extract Views (viewCount)
            Matcher viewsMatcher = Pattern.compile("\"viewCount\":\"(\\d+)\"").matcher(body);
            if (viewsMatcher.find()) {
                long views = Long.parseLong(viewsMatcher.group(1));
                meta.viewsCount = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(views);
            }

            // 3. Extract Title
            Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>").matcher(body);
            if (titleMatcher.find()) {
                String t = titleMatcher.group(1);
                if (t != null && t.endsWith(" - YouTube")) {
                    t = t.substring(0, t.length() - 10).trim();
                }
                meta.title = t;
            }
        } catch (Exception ex) {
            log.warn("Could not fetch YouTube metadata directly for videoId {}: {}", videoId, ex.getMessage());
        }
        return meta;
    }

    /**
     * Fetches video title via YouTube oEmbed API as fallback
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

    /**
     * Creates a YouTube-sourced video with automatic duration and views detection
     */
    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        String youtubeId = extractYouTubeId(request.getYoutubeUrl());
        if (youtubeId == null) {
            throw new IllegalArgumentException("رابط يوتيوب غير صالح. يرجى التأكد من الرابط.");
        }

        // Auto-fetch YouTube metadata (duration, views count, title)
        YouTubeMetadata meta = fetchYouTubeMetadata(youtubeId);

        String title = request.getTitle();
        if (title == null || title.trim().isEmpty()) {
            if (meta.title != null && !meta.title.isBlank()) {
                title = meta.title;
            } else {
                title = fetchYouTubeTitleOEmbed(request.getYoutubeUrl());
                if (title == null || title.trim().isEmpty()) {
                    title = "فيديو يوتيوب (" + youtubeId + ")";
                }
            }
        }

        String duration = (request.getDuration() != null && !request.getDuration().isBlank())
                ? request.getDuration()
                : (meta.duration != null ? meta.duration : "غير محدد");

        String viewsCount = (request.getViewsCount() != null && !request.getViewsCount().isBlank())
                ? request.getViewsCount()
                : (meta.viewsCount != null ? meta.viewsCount : "جديد");

        String category = request.getCategory() != null && !request.getCategory().isBlank()
                ? request.getCategory()
                : "lessons";

        String categoryName = request.getCategoryName() != null && !request.getCategoryName().isBlank()
                ? request.getCategoryName()
                : "دروس ومواعظ";

        boolean isFeatured = Boolean.TRUE.equals(request.getIsFeatured());
        if (isFeatured) {
            videoRepository.findAll().forEach(v -> {
                if (Boolean.TRUE.equals(v.getIsFeatured())) {
                    v.setIsFeatured(false);
                    videoRepository.save(v);
                }
            });
        }

        Video video = Video.builder()
                .videoSourceType("YOUTUBE")
                .youtubeUrl(request.getYoutubeUrl().trim())
                .youtubeId(youtubeId)
                .title(title.trim())
                .description(request.getDescription())
                .category(category)
                .categoryName(categoryName)
                .duration(duration)
                .viewsCount(viewsCount)
                .customPoster(request.getCustomPoster())
                .isFeatured(isFeatured)
                .build();

        Video saved = videoRepository.save(video);
        return videoMapper.toResponse(saved);
    }

    /**
     * Uploads and stores a local video file (MP4, WebM, etc.)
     */
    @Transactional
    public VideoResponse uploadLocalVideo(
            MultipartFile file,
            String title,
            String description,
            String category,
            String categoryName,
            String duration,
            String viewsCount,
            Boolean isFeatured
    ) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("يرجى اختيار ملف فيديو لرفعه.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            originalFilename = "video.mp4";
        }

        // Validate basic video extensions
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex).toLowerCase();
        }
        if (!extension.matches("\\.(mp4|webm|mov|mkv|avi|m4v)$")) {
            throw new IllegalArgumentException("صيغة الملف غير مدعومة. يرجى رفع ملف فيديو بصيغة (MP4, WebM, MOV, MKV).");
        }

        // Create upload directory if not exists
        Path uploadDirPath = Paths.get("uploads", "videos");
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        // Generate clean unique filename
        String safeUniqueFilename = UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis() + extension;
        Path targetPath = uploadDirPath.resolve(safeUniqueFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/uploads/videos/" + safeUniqueFilename;

        String finalTitle = title;
        if (finalTitle == null || finalTitle.trim().isEmpty()) {
            finalTitle = originalFilename.substring(0, dotIndex > 0 ? dotIndex : originalFilename.length());
        }

        String cat = (category != null && !category.isBlank()) ? category : "lessons";
        String catName = (categoryName != null && !categoryName.isBlank()) ? categoryName : "دروس ومواعظ";

        boolean featured = Boolean.TRUE.equals(isFeatured);
        if (featured) {
            videoRepository.findAll().forEach(v -> {
                if (Boolean.TRUE.equals(v.getIsFeatured())) {
                    v.setIsFeatured(false);
                    videoRepository.save(v);
                }
            });
        }

        // Auto-detect duration from file if not provided or set to generic text
        String finalDuration = duration;
        if (finalDuration == null || finalDuration.isBlank() || "فيديو مباشر".equals(finalDuration) || "غير محدد".equals(finalDuration)) {
            String extracted = extractMp4Duration(targetPath);
            if (extracted != null && !extracted.isBlank()) {
                finalDuration = extracted;
            } else {
                finalDuration = "00:00";
            }
        }

        String finalViews = (viewsCount != null && !viewsCount.isBlank()) ? viewsCount : "0";

        Video video = Video.builder()
                .videoSourceType("LOCAL")
                .fileUrl(fileUrl)
                .fileName(originalFilename)
                .fileSize(file.getSize())
                .title(finalTitle.trim())
                .description(description)
                .category(cat)
                .categoryName(catName)
                .duration(finalDuration)
                .viewsCount(finalViews)
                .isFeatured(featured)
                .build();

        Video saved = videoRepository.save(video);
        log.info("Saved local uploaded video: {} -> {} (duration: {})", originalFilename, fileUrl, finalDuration);
        return videoMapper.toResponse(saved);
    }

    /**
     * Extracts duration from MP4/MOV header atoms (moov -> mvhd) in pure Java.
     */
    private String extractMp4Duration(Path filePath) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(filePath.toFile(), "r")) {
            long fileLength = raf.length();
            long pos = 0;
            while (pos < fileLength - 8) {
                raf.seek(pos);
                int size = raf.readInt();
                byte[] typeBytes = new byte[4];
                raf.readFully(typeBytes);
                String type = new String(typeBytes, java.nio.charset.StandardCharsets.US_ASCII);

                long boxSize = (size & 0xFFFFFFFFL);
                if (boxSize == 1) {
                    boxSize = raf.readLong();
                } else if (boxSize == 0) {
                    boxSize = fileLength - pos;
                }

                if ("moov".equals(type)) {
                    long moovEnd = pos + boxSize;
                    long subPos = pos + 8;
                    while (subPos < moovEnd - 8) {
                        raf.seek(subPos);
                        int subSize = raf.readInt();
                        byte[] subTypeBytes = new byte[4];
                        raf.readFully(subTypeBytes);
                        String subType = new String(subTypeBytes, java.nio.charset.StandardCharsets.US_ASCII);

                        long subBoxSize = (subSize & 0xFFFFFFFFL);
                        if (subBoxSize == 1) {
                            subBoxSize = raf.readLong();
                        } else if (subBoxSize == 0) {
                            subBoxSize = moovEnd - subPos;
                        }

                        if ("mvhd".equals(subType)) {
                            int version = raf.readByte() & 0xFF;
                            raf.skipBytes(3); // flags
                            long timeScale;
                            long durationUnits;
                            if (version == 1) {
                                raf.skipBytes(16); // creation + mod
                                timeScale = raf.readInt() & 0xFFFFFFFFL;
                                durationUnits = raf.readLong();
                            } else {
                                raf.skipBytes(8); // creation + mod
                                timeScale = raf.readInt() & 0xFFFFFFFFL;
                                durationUnits = raf.readInt() & 0xFFFFFFFFL;
                            }

                            if (timeScale > 0) {
                                long totalSec = durationUnits / timeScale;
                                long hours = totalSec / 3600;
                                long mins = (totalSec % 3600) / 60;
                                long secs = totalSec % 60;
                                if (hours > 0) {
                                    return String.format("%d:%02d:%02d", hours, mins, secs);
                                } else {
                                    return String.format("%02d:%02d", mins, secs);
                                }
                            }
                            break;
                        }

                        if (subBoxSize <= 8) break;
                        subPos += subBoxSize;
                    }
                    break;
                }

                if (boxSize <= 8) break;
                pos += boxSize;
            }
        } catch (Exception ex) {
            log.warn("Could not extract MP4 duration for {}: {}", filePath, ex.getMessage());
        }
        return null;
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("الفيديو غير موجود برقم المعرف: " + id));

        // If local file, delete file from disk as well
        if ("LOCAL".equalsIgnoreCase(video.getVideoSourceType()) && video.getFileUrl() != null) {
            try {
                String relativePath = video.getFileUrl().replaceFirst("^/", "");
                Path filePath = Paths.get(relativePath);
                Files.deleteIfExists(filePath);
                log.info("Deleted local video file from disk: {}", filePath);
            } catch (Exception ex) {
                log.warn("Could not delete physical video file from disk: {}", ex.getMessage());
            }
        }

        videoRepository.delete(video);
    }
}
