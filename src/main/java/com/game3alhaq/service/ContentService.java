package com.game3alhaq.service;

import com.game3alhaq.dto.request.ContentRequests.PrayerTimesUpdateRequest;
import com.game3alhaq.dto.response.ContentResponses.HistoryResponse;
import com.game3alhaq.dto.response.ContentResponses.PrayerTimesResponse;
import com.game3alhaq.entity.PrayersAndHistory;
import com.game3alhaq.repository.PrayersAndHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ContentService {

    private final PrayersAndHistoryRepository prayersAndHistoryRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    public PrayerTimesResponse getPrayerTimes() {
        Optional<PrayersAndHistory> latestOpt = prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc();
        LocalDate today = LocalDate.now();

        if (latestOpt.isPresent()) {
            PrayersAndHistory existing = latestOpt.get();
            if (Boolean.TRUE.equals(existing.getManualOverride()) || today.equals(existing.getEffectiveDate())) {
                return mapToResponse(existing);
            }
        }

        PrayersAndHistory fetched = fetchAndPersistDailySchedule(latestOpt.orElse(null), today);
        return mapToResponse(fetched);
    }

    public PrayerTimesResponse updatePrayerTimes(PrayerTimesUpdateRequest req) {
        PrayersAndHistory entity = prayersAndHistoryRepository.findTopByOrderByEffectiveDateDesc()
                .orElseGet(PrayersAndHistory::new);
        entity.setFajrTime(cleanTime(req.getFajr()));
        entity.setSunriseTime(cleanTime(req.getSunrise()));
        entity.setDhuhrTime(cleanTime(req.getDhuhr()));
        entity.setAsrTime(cleanTime(req.getAsr()));
        entity.setMaghribTime(cleanTime(req.getMaghrib()));
        entity.setIshaTime(cleanTime(req.getIsha()));
        entity.setManualOverride(req.getManualOverride() != null ? req.getManualOverride() : true);
        entity.setEffectiveDate(LocalDate.now());

        PrayersAndHistory saved = prayersAndHistoryRepository.save(entity);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public HistoryResponse getHistory() {
        return HistoryResponse.builder()
                .mosqueName("جامع الحق ودار المناسبات")
                .location("ولاية السيب — الموالح الجنوبية، سلطنة عمان")
                .inaugurationYear(2011)
                .landArea("9,171 م²")
                .worshippersCapacity(1800)
                .majlisCapacity(700)
                .minarets("مئذنتان بارتفاع 24 متراً")
                .gatesCount(6)
                .overview(buildMosqueOverviewText())
                .facilities(buildFacilitiesList())
                .waqfProjects(buildWaqfProjectsList())
                .build();
    }

    private PrayersAndHistory fetchAndPersistDailySchedule(PrayersAndHistory existing, LocalDate today) {
        PrayersAndHistory target = (existing != null) ? existing : new PrayersAndHistory();
        applyAladhanTimings(target);
        target.setEffectiveDate(today);
        target.setManualOverride(false);
        return prayersAndHistoryRepository.save(target);
    }

    private void applyAladhanTimings(PrayersAndHistory target) {
        try {
            String url = "https://api.aladhan.com/v1/timingsByCity?city=Seeb&country=Oman&method=4";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                parseAladhanBody(response.getBody(), target);
                return;
            }
        } catch (Exception ex) {
            log.warn("Aladhan API fetch failed, falling back to default Seeb timings: {}", ex.getMessage());
        }
        applyDefaultSeebTimings(target);
    }

    @SuppressWarnings("unchecked")
    private void parseAladhanBody(Map body, PrayersAndHistory target) {
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        Map<String, Object> timings = (Map<String, Object>) data.get("timings");
        target.setFajrTime(cleanTime(timings.get("Fajr")));
        target.setSunriseTime(cleanTime(timings.get("Sunrise")));
        target.setDhuhrTime(cleanTime(timings.get("Dhuhr")));
        target.setAsrTime(cleanTime(timings.get("Asr")));
        target.setMaghribTime(cleanTime(timings.get("Maghrib")));
        target.setIshaTime(cleanTime(timings.get("Isha")));
    }

    private void applyDefaultSeebTimings(PrayersAndHistory target) {
        target.setFajrTime("05:00");
        target.setSunriseTime("06:15");
        target.setDhuhrTime("12:15");
        target.setAsrTime("15:35");
        target.setMaghribTime("18:05");
        target.setIshaTime("19:25");
    }

    private String cleanTime(Object val) {
        if (val == null) return "00:00";
        String str = val.toString().trim();
        return str.contains(" ") ? str.split(" ")[0] : str;
    }

    private PrayerTimesResponse mapToResponse(PrayersAndHistory entity) {
        return PrayerTimesResponse.builder()
                .fajr(entity.getFajrTime())
                .sunrise(entity.getSunriseTime())
                .dhuhr(entity.getDhuhrTime())
                .asr(entity.getAsrTime())
                .maghrib(entity.getMaghribTime())
                .isha(entity.getIshaTime())
                .manualOverride(entity.getManualOverride())
                .effectiveDate(entity.getEffectiveDate())
                .nextPrayer(computeNextPrayer(entity))
                .build();
    }

    private String computeNextPrayer(PrayersAndHistory entity) {
        try {
            LocalTime now = LocalTime.now();
            if (now.isBefore(parseTime(entity.getFajrTime()))) return "Fajr";
            if (now.isBefore(parseTime(entity.getSunriseTime()))) return "Sunrise";
            if (now.isBefore(parseTime(entity.getDhuhrTime()))) return "Dhuhr";
            if (now.isBefore(parseTime(entity.getAsrTime()))) return "Asr";
            if (now.isBefore(parseTime(entity.getMaghribTime()))) return "Maghrib";
            if (now.isBefore(parseTime(entity.getIshaTime()))) return "Isha";
            return "Fajr";
        } catch (Exception ex) {
            return "Fajr";
        }
    }

    private LocalTime parseTime(String time) {
        return LocalTime.parse(cleanTime(time));
    }

    private String buildMosqueOverviewText() {
        return "جامع الحق هو صرح إسلامي ومجتمعي متكامل بولاية السيب بسلطنة عمان، " +
                "يجمع بين عبادة الله عز وجل والعمل الخيري والتكافل الاجتماعي، " +
                "وتأسس عام 2011 لخدمة أهالي منطقة الموالح الجنوبية وما جاورها.";
    }

    private List<String> buildFacilitiesList() {
        return List.of(
                "المصلى الرئيسي للرجال يتسع لأكثر من 1800 مصلٍ",
                "مصلى النساء الموسع المجهز بالكامل بأنظمة صوتية وشاشات",
                "قاعة المجلس العام (دار المناسبات) مجهزة بمطبخ صناعي وفرن حلوى عمانية",
                "مبنى دورات المياه والمواضئ (22 دورة مياه، 70 صنبور وضوء)",
                "مكتب الإدارة وأنظمة المراقبة الذكية المتقدمة",
                "قاعات البرامج التعليمية وحلقات تحفيظ القرآن الكريم"
        );
    }

    private List<String> buildWaqfProjectsList() {
        return List.of(
                "مدرسة قرآنية نموذجية من طابقين",
                "مكتبة إسلامية عامة",
                "محلات تجارية وقفية استثمارية",
                "مساكن الأئمة والخطباء",
                "صالة طعام وقاعة ضيافة سفلية"
        );
    }
}
