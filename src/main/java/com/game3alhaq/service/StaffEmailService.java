package com.game3alhaq.service;

import com.game3alhaq.dto.request.StaffEmailRequests.CreateStaffEmailRequest;
import com.game3alhaq.dto.response.StaffEmailResponses.StaffEmailResponse;
import com.game3alhaq.entity.StaffEmail;
import com.game3alhaq.exception.ResourceNotFoundException;
import com.game3alhaq.repository.StaffEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffEmailService {

    private final StaffEmailRepository staffEmailRepository;

    @Transactional(readOnly = true)
    public List<StaffEmailResponse> getAllStaffEmails() {
        return staffEmailRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public StaffEmailResponse addStaffEmail(CreateStaffEmailRequest request) {
        String cleanEmail = request.getEmail().trim().toLowerCase();
        if (staffEmailRepository.existsByEmailIgnoreCase(cleanEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "البريد الإلكتروني مضاف مسبقاً في قائمة الموظفين");
        }

        StaffEmail entity = StaffEmail.builder()
                .fullName(request.getFullName().trim())
                .email(cleanEmail)
                .build();

        StaffEmail saved = staffEmailRepository.save(entity);
        log.info("Added new staff email: {} ({})", saved.getEmail(), saved.getFullName());
        return mapToResponse(saved);
    }

    @Transactional
    public void deleteStaffEmail(Long id) {
        StaffEmail staff = staffEmailRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff email not found with id: " + id));
        staffEmailRepository.delete(staff);
        log.info("Deleted staff email: id={}, email={}", id, staff.getEmail());
    }

    private StaffEmailResponse mapToResponse(StaffEmail s) {
        return StaffEmailResponse.builder()
                .id(s.getId())
                .fullName(s.getFullName())
                .email(s.getEmail())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
