package com.game3alhaq.service;

import com.game3alhaq.dto.request.MediaRequests.CreateVideoRequest;
import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.entity.Video;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.VideoMapper;
import com.game3alhaq.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final VideoMapper videoMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${youtube.api.key:}")
    private String youtubeApiKey;

    private static final Pattern YOUTUBE_PATTERN = Pattern.compile(
            "(?:youtube\\.com\\/(?:[^\\/]+\\/.+\\/|(?:v|e(?:mbed)?)\\/|.*[?&]v=)|youtu\\.be\\/|youtube\\.com\\/shorts\\/)([^\"&?\\/\\s]{11})",
            Pattern.CASE_INSENSITIVE
    );

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
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", id));
        return videoMapper.toResponse(video);
    }

    @Transactional
    public VideoResponse createVideo(CreateVideoRequest request) {
        String youtubeId = extractYouTubeId(request.getYoutubeUrl());
        if (youtubeId == null) {
            throw new IllegalArgumentException("رابط يوتيوب غير صالح. يرجى التأكد من الرابط.");
        }
        YouTubeMetadata meta = fetchYouTubeMetadata(youtubeId, request.getYoutubeUrl());
        boolean isFeatured = Boolean.TRUE.equals(request.getIsFeatured());
        if (isFeatured) {
            videoRepository.clearFeaturedVideos();
        }
        Video video = buildYouTubeVideo(request, youtubeId, meta, isFeatured);
        Video saved = videoRepository.save(video);
        return videoMapper.toResponse(saved);
    }

    @Transactional
    public VideoResponse uploadLocalVideo(MultipartFile file, String title, String description,
                                          String category, String categoryName, String duration,
                                          String viewsCount, Boolean isFeatured) throws IOException {
        validateUploadedFile(file);
        Path targetDir = resolveUploadDirectory();
        String safeName = generateSafeFilename(file.getOriginalFilename());
        Path targetPath = targetDir.resolve(safeName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        boolean featured = Boolean.TRUE.equals(isFeatured);
        if (featured) {
            videoRepository.clearFeaturedVideos();
        }
        Video video = buildLocalVideoEntity(file, safeName, targetPath, title, description,
                category, categoryName, duration, viewsCount, featured);
        Video saved = videoRepository.save(video);
        return videoMapper.toResponse(saved);
    }

    @Transactional
    public VideoResponse setFeaturedVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", id));
        videoRepository.clearFeaturedVideos();
        videoRepository.setFeaturedVideo(id);
        video.setIsFeatured(true);
        return videoMapper.toResponse(video);
    }

    @Transactional
    public void deleteVideo(Long id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video", "id", id));
        deleteLocalPhysicalFile(video);
        videoRepository.delete(video);
    }

    public String extractYouTubeId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }
        String trimmed = url.trim();
        if (trimmed.matches("^[a-zA-Z0-9_-]{11}$")) {
            return trimmed;
        }
        Matcher matcher = YOUTUBE_PATTERN.matcher(trimmed);
        return matcher.find() ? matcher.group(1) : null;
    }

    public YouTubeMetadata fetchYouTubeMetadata(String videoId, String videoUrl) {
        YouTubeMetadata meta = new YouTubeMetadata();
        if (youtubeApiKey != null && !youtubeApiKey.isBlank()) {
            fetchFromYouTubeDataApi(videoId, meta);
        }
        if (meta.title == null || meta.title.isBlank()) {
            meta.title = fetchTitleFromOEmbed(videoUrl);
        }
        return meta;
    }

    public String parseIsoDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) {
            return null;
        }
        try {
            Duration d = Duration.parse(isoDuration);
            long totalSeconds = d.toSeconds();
            long hours = totalSeconds / 3600;
            long minutes = (totalSeconds % 3600) / 60;
            long seconds = totalSeconds % 60;
            if (hours > 0) {
                return String.format("%d:%02d:%02d", hours, minutes, seconds);
            }
            return String.format("%02d:%02d", minutes, seconds);
        } catch (Exception ex) {
            log.warn("Failed to parse ISO duration '{}': {}", isoDuration, ex.getMessage());
            return null;
        }
    }

    public static class YouTubeMetadata {
        public String title;
        public String duration;
        public String viewsCount;
    }

    private void fetchFromYouTubeDataApi(String videoId, YouTubeMetadata meta) {
        try {
            String url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails,statistics&id="
                    + videoId + "&key=" + youtubeApiKey;
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseYouTubeApiResponse(response.getBody(), meta);
            }
        } catch (Exception ex) {
            log.warn("YouTube Data API call failed for id {}: {}", videoId, ex.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void parseYouTubeApiResponse(Map body, YouTubeMetadata meta) {
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        if (items == null || items.isEmpty()) return;
        Map<String, Object> item = items.get(0);
        extractSnippetTitle((Map<String, Object>) item.get("snippet"), meta);
        extractContentDetailsDuration((Map<String, Object>) item.get("contentDetails"), meta);
        extractStatisticsViews((Map<String, Object>) item.get("statistics"), meta);
    }

    private void extractSnippetTitle(Map<String, Object> snippet, YouTubeMetadata meta) {
        if (snippet != null && snippet.get("title") != null) {
            meta.title = snippet.get("title").toString();
        }
    }

    private void extractContentDetailsDuration(Map<String, Object> contentDetails, YouTubeMetadata meta) {
        if (contentDetails != null && contentDetails.get("duration") != null) {
            meta.duration = parseIsoDuration(contentDetails.get("duration").toString());
        }
    }

    private void extractStatisticsViews(Map<String, Object> statistics, YouTubeMetadata meta) {
        if (statistics != null && statistics.get("viewCount") != null) {
            try {
                long views = Long.parseLong(statistics.get("viewCount").toString());
                meta.viewsCount = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(views);
            } catch (NumberFormatException ignored) {}
        }
    }

    public String fetchTitleFromOEmbed(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) return null;
        try {
            String oembedUrl = "https://www.youtube.com/oembed?url=" + videoUrl + "&format=json";
            ResponseEntity<Map> response = restTemplate.getForEntity(oembedUrl, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object title = response.getBody().get("title");
                return title != null ? title.toString() : null;
            }
        } catch (Exception ex) {
            log.warn("YouTube oEmbed call failed for {}: {}", videoUrl, ex.getMessage());
        }
        return null;
    }

    private Video buildYouTubeVideo(CreateVideoRequest req, String yId, YouTubeMetadata meta, boolean isFeatured) {
        String title = resolveVideoTitle(req.getTitle(), meta.title, yId);
        String duration = (req.getDuration() != null && !req.getDuration().isBlank())
                ? req.getDuration() : (meta.duration != null ? meta.duration : "غير محدد");
        String views = (req.getViewsCount() != null && !req.getViewsCount().isBlank())
                ? req.getViewsCount() : (meta.viewsCount != null ? meta.viewsCount : "جديد");
        return Video.builder()
                .videoSourceType("YOUTUBE").youtubeUrl(req.getYoutubeUrl().trim()).youtubeId(yId)
                .title(title.trim()).description(req.getDescription())
                .category(req.getCategory() != null ? req.getCategory() : "lessons")
                .categoryName(req.getCategoryName() != null ? req.getCategoryName() : "دروس ومواعظ")
                .duration(duration).viewsCount(views).customPoster(req.getCustomPoster())
                .isFeatured(isFeatured).build();
    }

    private String resolveVideoTitle(String reqTitle, String metaTitle, String yId) {
        if (reqTitle != null && !reqTitle.isBlank()) return reqTitle;
        if (metaTitle != null && !metaTitle.isBlank()) return metaTitle;
        return "فيديو يوتيوب (" + yId + ")";
    }

    private void validateUploadedFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("يرجى اختيار ملف فيديو لرفعه.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("نوع المحتوى يجب أن يكون فيديو.");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!filename.matches(".*\\.(mp4|webm|mov|mkv|avi|m4v)$")) {
            throw new IllegalArgumentException("صيغة الملف غير مدعومة. يرجى رفع ملف فيديو بصيغة (MP4, WebM, MOV, MKV).");
        }
    }

    private Path resolveUploadDirectory() throws IOException {
        Path dir = Paths.get(uploadDir, "videos");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }

    private String generateSafeFilename(String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase();
        }
        return UUID.randomUUID().toString().substring(0, 8) + "_" + System.currentTimeMillis() + ext;
    }

    private Video buildLocalVideoEntity(MultipartFile file, String safeName, Path targetPath,
                                        String title, String desc, String cat, String catName,
                                        String duration, String views, boolean featured) {
        String finalTitle = resolveLocalTitle(title, file.getOriginalFilename());
        String finalDuration = resolveLocalDuration(duration, targetPath);
        return Video.builder()
                .videoSourceType("LOCAL")
                .fileUrl("/uploads/videos/" + safeName)
                .fileName(file.getOriginalFilename() != null ? file.getOriginalFilename() : safeName)
                .fileSize(file.getSize())
                .title(finalTitle)
                .description(desc)
                .category(cat != null && !cat.isBlank() ? cat : "lessons")
                .categoryName(catName != null && !catName.isBlank() ? catName : "دروس ومواعظ")
                .duration(finalDuration)
                .viewsCount(views != null && !views.isBlank() ? views : "0")
                .isFeatured(featured)
                .build();
    }

    private String resolveLocalTitle(String title, String originalFilename) {
        if (title != null && !title.isBlank()) return title.trim();
        if (originalFilename == null) return "فيديو محلي";
        int dot = originalFilename.lastIndexOf('.');
        return dot > 0 ? originalFilename.substring(0, dot) : originalFilename;
    }

    private String resolveLocalDuration(String duration, Path path) {
        if (duration != null && !duration.isBlank() && !"فيديو مباشر".equals(duration) && !"غير محدد".equals(duration)) {
            return duration;
        }
        String extracted = extractMp4Duration(path);
        return extracted != null ? extracted : "00:00";
    }

    private void deleteLocalPhysicalFile(Video video) {
        if (!"LOCAL".equalsIgnoreCase(video.getVideoSourceType()) || video.getFileUrl() == null) {
            return;
        }
        try {
            String filename = video.getFileUrl().replaceFirst(".*/", "");
            Path filePath = Paths.get(uploadDir, "videos", filename);
            Files.deleteIfExists(filePath);
            log.info("Deleted local video file: {}", filePath);
        } catch (Exception ex) {
            log.warn("Could not delete physical video file: {}", ex.getMessage());
        }
    }

    private String extractMp4Duration(Path filePath) {
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
            return findMoovAndDuration(raf, raf.length());
        } catch (Exception ex) {
            log.warn("Could not extract MP4 duration for {}: {}", filePath, ex.getMessage());
            return null;
        }
    }

    private String findMoovAndDuration(RandomAccessFile raf, long fileLength) throws IOException {
        long pos = 0;
        while (pos < fileLength - 8) {
            raf.seek(pos);
            int size = raf.readInt();
            byte[] type = new byte[4];
            raf.readFully(type);
            long boxSize = (size == 1) ? raf.readLong() : (size == 0 ? fileLength - pos : (size & 0xFFFFFFFFL));
            if ("moov".equals(new String(type, StandardCharsets.US_ASCII))) {
                return parseMvhdInsideMoov(raf, pos + 8, pos + boxSize);
            }
            if (boxSize <= 8) break;
            pos += boxSize;
        }
        return null;
    }

    private String parseMvhdInsideMoov(RandomAccessFile raf, long startPos, long endPos) throws IOException {
        long pos = startPos;
        while (pos < endPos - 8) {
            raf.seek(pos);
            int size = raf.readInt();
            byte[] type = new byte[4];
            raf.readFully(type);
            long boxSize = (size == 1) ? raf.readLong() : (size == 0 ? endPos - pos : (size & 0xFFFFFFFFL));
            if ("mvhd".equals(new String(type, StandardCharsets.US_ASCII))) {
                return readMvhdDuration(raf);
            }
            if (boxSize <= 8) break;
            pos += boxSize;
        }
        return null;
    }

    private String readMvhdDuration(RandomAccessFile raf) throws IOException {
        int version = raf.readByte() & 0xFF;
        raf.skipBytes(3);
        long timeScale = (version == 1) ? (raf.skipBytes(16) == 16 ? raf.readInt() & 0xFFFFFFFFL : 0)
                : (raf.skipBytes(8) == 8 ? raf.readInt() & 0xFFFFFFFFL : 0);
        long durationUnits = (version == 1) ? raf.readLong() : (raf.readInt() & 0xFFFFFFFFL);
        if (timeScale > 0) {
            long totalSec = durationUnits / timeScale;
            long hours = totalSec / 3600;
            long mins = (totalSec % 3600) / 60;
            long secs = totalSec % 60;
            return (hours > 0) ? String.format("%d:%02d:%02d", hours, mins, secs)
                    : String.format("%02d:%02d", mins, secs);
        }
        return null;
    }
}
