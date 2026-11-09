package com.game3alhaq.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class StaffEmailRequests {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateStaffEmailRequest {
        @NotBlank(message = "الاسم مطلوب")
        @Size(max = 150, message = "الاسم لا يجب أن يتجاوز 150 حرفاً")
        private String fullName;

        @NotBlank(message = "البريد الإلكتروني مطلوب")
        @Email(message = "صيغة البريد الإلكتروني غير صحيحة")
        @Size(max = 150, message = "البريد الإلكتروني لا يجب أن يتجاوز 150 حرفاً")
        private String email;
    }
}
