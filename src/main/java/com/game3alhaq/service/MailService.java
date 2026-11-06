package com.game3alhaq.service;

import com.game3alhaq.entity.Booking;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;

    private static final String CONFIRMATION_EMAIL_TEMPLATE = """
            <div dir="rtl" style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #1a4d2e; text-align: center;">جامع الحق — دار المناسبات</h2>
                <p>السلام عليكم ورحمة الله وبركاته،</p>
                <p>مرحباً <strong>%s</strong>، تم تأكيد حجزك بنجاح في جامع الحق.</p>
                <table style="width: 100%%; border-collapse: collapse; margin-top: 15px;">
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>رمز الحجز:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee; color: #1a4d2e; font-weight: bold;">%s</td></tr>
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>القاعة:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td></tr>
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>المناسبة:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td></tr>
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>التاريخ:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s</td></tr>
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>الوقت:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee;">من %d:00 إلى %d:00</td></tr>
                    <tr><td style="padding: 8px; border-bottom: 1px solid #eee;"><strong>المبلغ الإجمالي:</strong></td><td style="padding: 8px; border-bottom: 1px solid #eee;">%s %s</td></tr>
                </table>
                <p style="margin-top: 20px; color: #555; text-align: center;">شاكرين لكم حسن تعاونكم — ولاية السيب، سلطنة عمان</p>
            </div>
            """;

    private static final String PASSWORD_RESET_OTP_TEMPLATE = """
            <div dir="rtl" style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                <h2 style="color: #1a4d2e; text-align: center;">جامع الحق — استعادة كلمة المرور</h2>
                <p>السلام عليكم ورحمة الله وبركاته،</p>
                <p>لقد تلقينا طلباً لإعادة تعيين كلمة المرور الخاصة بحسابكم على منصة جامع الحق.</p>
                <div style="text-align: center; margin: 30px 0;">
                    <span style="display: inline-block; font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #1a4d2e; background: #f0f7f2; padding: 12px 24px; border-radius: 6px; border: 1px dashed #1a4d2e;">%s</span>
                </div>
                <p style="color: #d9534f; font-weight: bold; text-align: center;">رمز التحقق صالح لمدة 15 دقيقة فقط.</p>
                <p style="color: #777; font-size: 13px; text-align: center;">إذا لم تقم بطلب هذا الرمز، يُرجى تجاهل هذه الرسالة أو التواصل مع إدارة الجامع.</p>
                <p style="margin-top: 20px; color: #555; text-align: center;">إدارة جامع الحق — ولاية السيب، سلطنة عمان</p>
            </div>
            """;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public MailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (!canSendEmail(booking)) {
            return;
        }
        try {
            sendHtmlMessage(booking);
        } catch (Exception ex) {
            log.warn("Failed to send booking confirmation email for code {}: {}",
                    booking.getCode(), ex.getMessage());
        }
    }

    @Async
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        if (!canSendOtp(toEmail, otpCode)) {
            return;
        }
        try {
            sendOtpMimeMessage(toEmail, otpCode);
        } catch (Exception ex) {
            log.warn("Failed to send password reset OTP to {}: {}", toEmail, ex.getMessage());
        }
    }

    private boolean canSendOtp(String toEmail, String otpCode) {
        if (mailSender == null) {
            log.info("JavaMailSender bean not configured. Skipping reset OTP email.");
            return false;
        }
        return toEmail != null && !toEmail.isBlank() && otpCode != null && !otpCode.isBlank();
    }

    private void sendOtpMimeMessage(String toEmail, String otpCode) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@game3alhaq.om";
        helper.setFrom(sender, "جامع الحق — دار المناسبات");
        helper.setTo(toEmail);
        helper.setSubject("رمز التحقق لإعادة تعيين كلمة المرور — جامع الحق");
        helper.setText(PASSWORD_RESET_OTP_TEMPLATE.formatted(otpCode), true);

        mailSender.send(message);
        log.info("Password reset OTP email sent successfully to {}", toEmail);
    }

    private boolean canSendEmail(Booking booking) {
        if (mailSender == null) {
            log.info("JavaMailSender bean not configured. Skipping confirmation email.");
            return false;
        }
        return booking != null && booking.getEmail() != null && !booking.getEmail().isBlank();
    }

    private void sendHtmlMessage(Booking booking) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "noreply@game3alhaq.om";
        helper.setFrom(sender, "جامع الحق — دار المناسبات");
        helper.setTo(booking.getEmail());
        helper.setSubject("تأكيد حجز قاعة جامع الحق — رمز الحجز: " + booking.getCode());
        helper.setText(buildConfirmationHtml(booking), true);

        mailSender.send(message);
        log.info("Booking confirmation email sent successfully to {}", booking.getEmail());
    }

    private String buildConfirmationHtml(Booking b) {
        String occasion = "KATB_KETAB".equalsIgnoreCase(b.getBookingType()) ? "عقد قران" : "عزاء";
        return CONFIRMATION_EMAIL_TEMPLATE.formatted(
                b.getFullName(), b.getCode(), b.getHall().getName(),
                occasion, b.getEventDate(), b.getStartHour(), b.getEndHour(),
                b.getTotalAmount(), b.getCurrency());
    }
}
