package com.game3alhaq.service;

import com.game3alhaq.dto.request.MediaRequests.CreateVideoRequest;
import com.game3alhaq.dto.response.MediaResponses.VideoResponse;
import com.game3alhaq.entity.Video;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.mapper.VideoMapper;
import com.game3alhaq.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoMapper videoMapper;

    @InjectMocks
    private VideoService videoService;

    private Video sampleVideo;

    @BeforeEach
    void setUp() {
        sampleVideo = Video.builder()
                .id(1L)
                .videoSourceType("YOUTUBE")
                .youtubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .youtubeId("dQw4w9WgXcQ")
                .title("Test Lecture")
                .isFeatured(false)
                .build();
    }

    @Test
    @DisplayName("Should extract YouTube ID from various valid URL patterns")
    void testExtractYouTubeId() {
        assertEquals("dQw4w9WgXcQ", videoService.extractYouTubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", videoService.extractYouTubeId("https://youtu.be/dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", videoService.extractYouTubeId("https://www.youtube.com/shorts/dQw4w9WgXcQ"));
        assertEquals("dQw4w9WgXcQ", videoService.extractYouTubeId("dQw4w9WgXcQ"));
        assertNull(videoService.extractYouTubeId("https://invalid-url.com"));
        assertNull(videoService.extractYouTubeId(""));
        assertNull(videoService.extractYouTubeId(null));
    }

    @Test
    @DisplayName("Should parse ISO-8601 duration correctly")
    void testParseIsoDuration() {
        assertEquals("45:02", videoService.parseIsoDuration("PT45M2S"));
        assertEquals("1:15:30", videoService.parseIsoDuration("PT1H15M30S"));
        assertEquals("05:00", videoService.parseIsoDuration("PT5M"));
        assertNull(videoService.parseIsoDuration(null));
        assertNull(videoService.parseIsoDuration("invalid"));
    }

    @Test
    @DisplayName("Should create YouTube video successfully and clear previous featured if isFeatured is true")
    void testCreateVideo() {
        CreateVideoRequest request = CreateVideoRequest.builder()
                .youtubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
                .title("Friday Sermon")
                .isFeatured(true)
                .build();

        when(videoRepository.save(any(Video.class))).thenReturn(sampleVideo);
        when(videoMapper.toResponse(any(Video.class))).thenReturn(
                VideoResponsesHelper.createResponse(1L, "Friday Sermon", true));

        VideoResponse response = videoService.createVideo(request);

        assertNotNull(response);
        assertEquals("Friday Sermon", response.getTitle());
        verify(videoRepository, times(1)).clearFeaturedVideos();
        verify(videoRepository, times(1)).save(any(Video.class));
    }

    @Test
    @DisplayName("Should atomically set video as featured")
    void testSetFeaturedVideo() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));
        when(videoMapper.toResponse(sampleVideo)).thenReturn(
                VideoResponsesHelper.createResponse(1L, "Test Lecture", true));

        VideoResponse response = videoService.setFeaturedVideo(1L);

        assertNotNull(response);
        verify(videoRepository, times(1)).clearFeaturedVideos();
        verify(videoRepository, times(1)).setFeaturedVideo(1L);
    }

    @Test
    @DisplayName("Should delete video by ID successfully")
    void testDeleteVideo() {
        when(videoRepository.findById(1L)).thenReturn(Optional.of(sampleVideo));

        videoService.deleteVideo(1L);

        verify(videoRepository, times(1)).delete(sampleVideo);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent video")
    void testDeleteVideoNotFound() {
        when(videoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> videoService.deleteVideo(99L));
        verify(videoRepository, never()).delete(any());
    }

    private static class VideoResponsesHelper {
        static VideoResponse createResponse(Long id, String title, boolean isFeatured) {
            return VideoResponse.builder()
                    .id(id)
                    .title(title)
                    .isFeatured(isFeatured)
                    .build();
        }
    }
}
