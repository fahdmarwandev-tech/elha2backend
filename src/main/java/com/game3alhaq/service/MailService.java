package com.game3alhaq.service;

import com.game3alhaq.entity.Booking;
import com.game3alhaq.entity.StaffEmail;
import com.game3alhaq.repository.StaffEmailRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Mail Service for Game3 Al-Haq / Dar Monasbat platform.
 * Dispatches luxury confirmation emails to customers and mosque staff members.
 * Modeled after the Re-Sapori luxury email service architecture.
 */
@Slf4j
@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final StaffEmailRepository staffEmailRepository;

    @Value("${spring.mail.username:resapori.web@gmail.com}")
    private String fromEmail = "resapori.web@gmail.com";

    private static final DateTimeFormatter AR_DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", new Locale("ar"));

    public MailService(@Autowired(required = false) JavaMailSender mailSender,
                       @Autowired(required = false) StaffEmailRepository staffEmailRepository) {
        this.mailSender = mailSender;
        this.staffEmailRepository = staffEmailRepository;
    }

    /**
     * Sends booking confirmation HTML email to the customer who made the reservation,
     * and simultaneously sends notification emails to all staff emails registered in the database.
     */
    @Async
    public void sendBookingConfirmation(Booking booking) {
        if (!canSendEmail(booking)) {
            log.info("Skipping booking confirmation email: mailSender not available or booking invalid.");
            return;
        }

        // 1. Send confirmation email to the customer
        if (booking.getEmail() != null && !booking.getEmail().isBlank()) {
            String customerEmail = booking.getEmail().trim();
            try {
                sendHtmlBookingConfirmation(booking, customerEmail, false);
                log.info("Luxury booking confirmation email successfully dispatched to customer: {} for booking: {}",
                        customerEmail, booking.getCode());
            } catch (Exception ex) {
                log.warn("Failed to dispatch booking confirmation email via SMTP to customer {}: {}",
                        customerEmail, ex.getMessage());
            }
        }

        // 2. Dispatch notification email simultaneously to all staff emails
        if (staffEmailRepository != null) {
            try {
                List<StaffEmail> staffList = staffEmailRepository.findAll();
                String customerEmail = booking.getEmail() != null ? booking.getEmail().trim() : "";

                for (StaffEmail staff : staffList) {
                    if (staff != null && staff.getEmail() != null && !staff.getEmail().isBlank()) {
                        String staffEmailAddr = staff.getEmail().trim();
                        // Prevent duplicate if staff email is identical to customer email
                        if (!staffEmailAddr.equalsIgnoreCase(customerEmail)) {
                            try {
                                sendHtmlBookingConfirmation(booking, staffEmailAddr, true);
                                log.info("Staff notification email successfully dispatched to: {} for booking: {}",
                                        staffEmailAddr, booking.getCode());
                            } catch (Exception ex) {
                                log.warn("Failed to dispatch booking notification to staff {}: {}",
                                        staffEmailAddr, ex.getMessage());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to retrieve staff emails for booking notification: {}", e.getMessage());
            }
        }
    }

    /**
     * Sends password reset OTP email to user.
     */
    @Async
    public void sendPasswordResetOtp(String toEmail, String otpCode) {
        if (!canSendOtp(toEmail, otpCode)) {
            return;
        }
        try {
            sendOtpHtmlMessage(toEmail, otpCode);
        } catch (Exception ex) {
            log.warn("Failed to send password reset OTP to {}: {}", toEmail, ex.getMessage());
        }
    }

    private boolean canSendEmail(Booking booking) {
        if (mailSender == null) {
            log.info("JavaMailSender bean not configured. Skipping confirmation email.");
            return false;
        }
        return booking != null;
    }

    private boolean canSendOtp(String toEmail, String otpCode) {
        if (mailSender == null) {
            log.info("JavaMailSender bean not configured. Skipping reset OTP email.");
            return false;
        }
        return toEmail != null && !toEmail.isBlank() && otpCode != null && !otpCode.isBlank();
    }

    private void sendHtmlBookingConfirmation(Booking booking, String recipientEmail, boolean isStaff) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "resapori.web@gmail.com";
        helper.setFrom(sender, "جامع الحق — دار المناسبات");
        helper.setTo(recipientEmail.trim());

        String typeStr = resolveOccasionTitle(booking.getBookingType());
        String subject = (isStaff ? "[إشعار إدارة] حجز جديد: " : "تأكيد حجز ")
                + typeStr + " — جامع الحق [" + escapeHtml(booking.getCode()) + "]";
        helper.setSubject(subject);

        String htmlContent = buildLuxuryBookingConfirmationHtml(booking, isStaff);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private void sendOtpHtmlMessage(String toEmail, String otpCode) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

        String sender = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "marwanyasserhassan7@gmail.com";
        helper.setFrom(sender, "جامع الحق — دار المناسبات");
        helper.setTo(toEmail.trim());
        helper.setSubject("رمز التحقق لإعادة تعيين كلمة المرور — جامع الحق");

        String html = buildLuxuryOtpHtml(otpCode);
        helper.setText(html, true);

        mailSender.send(message);
        log.info("Password reset OTP email sent successfully to: {}", toEmail);
    }

    /**
     * Builds luxury HTML email for booking confirmation.
     * Modeled after Re-Sapori's buildOrderConfirmedEmail template.
     */
    private String buildLuxuryBookingConfirmationHtml(Booking b, boolean isStaff) {
        String customerName = escapeHtml(b.getFullName() != null ? b.getFullName() : "ضيفنا الكريم");
        String bookingCode = escapeHtml(b.getCode() != null ? b.getCode() : "HAQ-BOOKING");
        String occasionTitle = resolveOccasionTitle(b.getBookingType());
        String hallName = escapeHtml(b.getHall() != null ? b.getHall().getName() : "قاعة المجلس العام");

        // Format dates
        String dateDisplay;
        if (b.getEndDate() != null && !b.getEndDate().equals(b.getEventDate())) {
            dateDisplay = b.getEventDate() + " إلى " + b.getEndDate() + " (3 أيام متتالية)";
        } else if (b.getEventDate() != null) {
            try {
                dateDisplay = b.getEventDate().format(AR_DATE_FORMAT);
            } catch (Exception e) {
                dateDisplay = b.getEventDate().toString();
            }
        } else {
            dateDisplay = "الموعد المحدد";
        }

        // Format hours
        String timeDisplay = formatHoursDisplay(b.getStartHour(), b.getEndHour(), b.getDurationHours());
        String totalAmountStr = b.getTotalAmount() != null ? b.getTotalAmount().toPlainString() : "0.00";
        String currencyStr = escapeHtml(b.getCurrency() != null ? b.getCurrency() : "OMR");
        String phoneStr = escapeHtml(b.getPhone() != null ? b.getPhone() : "غير محدد");

        // Couple's names (اسما العروسين) or deceased name (اسم المتوفى) / occasion notes
        String notesRow = "";
        if (b.getNotes() != null && !b.getNotes().trim().isBlank()) {
            boolean isKatb = "katbketab".equalsIgnoreCase(b.getBookingType());
            String notesLabel = isKatb ? "اسما العروسين:" : "اسم المتوفى رحمه الله:";
            notesRow = """
                              <tr>
                                <td style="color: #8EA3BF;">%s</td>
                                <td style="color: #FFE8B8; font-weight: 800;">%s</td>
                              </tr>
                """.formatted(notesLabel, escapeHtml(b.getNotes().trim()));
        }

        String statusBadgeText = isStaff ? "● إشعار حجز جديد معتمد — إدارة ومسؤولي جامع الحق" : "● تم تأكيد الحجز والاعتماد بنجاح";
        String greetingHeader = isStaff ? "إشعار إداري: تم اعتماد حجز جديد باسم " + customerName : "مرحباً " + customerName + "، تم اعتماد حجزكم المبارك!";
        String greetingParagraph = isStaff
                ? "السلام عليكم ورحمة الله وبركاته، نود إحاطة إدارتكم الموقرة بأنه قد تم تسجيل وتأكيد حجز جديد في دار المناسبات وفق البيانات والتفاصيل التالية:"
                : "السلام عليكم ورحمة الله وبركاته، يسر إدارة جامع الحق إفادتكم بأنه قد تم تسجيل وتأكيد حجزكم في دار المناسبات وفق البيانات والتفاصيل الموضحة أدناه:";
        String replyToEmail = (fromEmail != null && !fromEmail.isBlank()) ? fromEmail : "resapori.web@gmail.com";

        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>تأكيد الحجز - جامع الحق</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #050A14; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, 'Traditional Arabic', sans-serif; -webkit-font-smoothing: antialiased; direction: rtl; text-align: right;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #050A14; padding: 40px 15px;">
                <tr>
                  <td align="center">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 600px; background-color: #0A1628; border: 1px solid #1E3A63; border-radius: 16px; overflow: hidden; box-shadow: 0 15px 40px rgba(0,0,0,0.8);">
                      
                      <!-- Header Branding (modeled after Re-Sapori) -->
                      <tr>
                        <td align="center" style="padding: 35px 30px 20px 30px; background: linear-gradient(180deg, #0D1A2D 0%%, #0A1628 100%%); border-bottom: 1px solid rgba(212, 175, 133, 0.3);">
                          <div style="font-size: 26px; font-weight: 900; letter-spacing: 1px; color: #D4AF85; margin-bottom: 6px;">جامع الحق • دار المناسبات</div>
                          <div style="font-size: 12px; letter-spacing: 2px; color: #8EA3BF; font-weight: bold;">صرح إيماني ومنارة مجتمعية • ولاية السيب</div>
                        </td>
                      </tr>

                      <!-- Confirmation Badge & Greeting -->
                      <tr>
                        <td style="padding: 30px 35px 20px 35px; color: #FAF7F0;">
                          <div style="text-align: center; margin-bottom: 20px;">
                            <span style="display: inline-block; background: rgba(16, 185, 129, 0.15); border: 1px solid #10B981; color: #34D399; padding: 6px 20px; border-radius: 20px; font-size: 12px; font-weight: 800; letter-spacing: 1px;">
                              %s
                            </span>
                          </div>

                          <h1 style="font-size: 22px; font-weight: 800; color: #FFFFFF; margin: 0 0 12px 0; text-align: center;">
                            %s
                          </h1>
                          <p style="font-size: 14px; line-height: 1.7; color: #8EA3BF; margin: 0 0 25px 0; text-align: center;">
                            %s
                          </p>

                          <!-- Booking Reference Card (Similar to Re-Sapori OTP / Order box) -->
                          <div style="background: #050B14; border: 1px dashed #D4AF85; border-radius: 12px; padding: 20px 15px; text-align: center; margin: 0 0 25px 0;">
                            <div style="font-size: 12px; font-weight: bold; color: #8EA3BF; margin-bottom: 6px; letter-spacing: 1px;">رمز الحجز المعتمد (Booking Reference)</div>
                            <div style="font-family: 'Courier New', Courier, monospace; font-size: 28px; font-weight: 900; letter-spacing: 4px; color: #FFE8B8;">
                              %s
                            </div>
                            <div style="font-size: 11px; color: #8EA3BF; margin-top: 6px;">يُرجى إبراز هذا الرمز عند مراجعة إدارة الجامع أو الاستقبال</div>
                          </div>

                          <!-- Reservation Metadata Table (modeled after Re-Sapori order details grid) -->
                          <div style="background-color: #060D1A; border: 1px solid #1E3A63; border-radius: 12px; padding: 18px 20px; margin-bottom: 25px;">
                            <div style="font-size: 13px; font-weight: 800; color: #D4AF85; margin-bottom: 14px; border-bottom: 1px solid #1E3A63; padding-bottom: 8px;">
                              📋 تفاصيل وبيانات الحجز
                            </div>
                            <table width="100%%" border="0" cellspacing="0" cellpadding="6" style="font-size: 13px; line-height: 1.6;">
                              <tr>
                                <td style="color: #8EA3BF; width: 35%%;">نوع المناسبة:</td>
                                <td style="color: #FFE8B8; font-weight: 800;">%s</td>
                              </tr>
                              %s
                              <tr>
                                <td style="color: #8EA3BF;">القاعة المخصصة:</td>
                                <td style="color: #FAF7F0; font-weight: 700;">%s</td>
                              </tr>
                              <tr>
                                <td style="color: #8EA3BF;">التاريخ:</td>
                                <td style="color: #FAF7F0; font-weight: 700;">%s</td>
                              </tr>
                              <tr>
                                <td style="color: #8EA3BF;">التوقيت والمدة:</td>
                                <td style="color: #FFE8B8; font-weight: 700;">%s</td>
                              </tr>
                              <tr>
                                <td style="color: #8EA3BF;">اسم صاحب الحجز:</td>
                                <td style="color: #FAF7F0;">%s</td>
                              </tr>
                              <tr>
                                <td style="color: #8EA3BF;">رقم الهاتف:</td>
                                <td style="color: #FAF7F0; font-family: monospace;" dir="ltr">%s</td>
                              </tr>
                            </table>
                          </div>

                          <!-- Total Paid Summary (modeled after Re-Sapori total due highlight) -->
                          <div style="background: linear-gradient(135deg, #0D1A2D 0%%, #122849 100%%); border: 1px solid #D4AF85; border-radius: 12px; padding: 18px 22px; margin-bottom: 25px;">
                            <table width="100%%" border="0" cellspacing="0" cellpadding="0">
                              <tr>
                                <td style="color: #FAF7F0; font-size: 15px; font-weight: 700;">إجمالي المبلغ المدفوع:</td>
                                <td align="left" style="color: #FFE8B8; font-size: 24px; font-weight: 900; font-family: -apple-system, sans-serif;">
                                  %s <span style="font-size: 14px; font-weight: normal; color: #D4AF85;">%s</span>
                                </td>
                              </tr>
                            </table>
                          </div>

                          <!-- Guidelines & Etiquette Notice -->
                          <div style="background-color: #060D1A; border: 1px solid #1E3A63; border-radius: 10px; padding: 14px 18px; margin-bottom: 25px; font-size: 12px; color: #8EA3BF; line-height: 1.7;">
                            <strong style="color: #D4AF85; display: block; margin-bottom: 4px;">توجيهات عامة:</strong>
                            • يُرجى التنسيق المسبق مع إدارة الجامع قبل الموعد لاستلام مفاتيح القاعة والترتيبات الصوتية.<br>
                            • المحافظة على وقار وسكينة بيت الله الحرام ونظافة مرافق القاعة والمجلس.<br>
                            • الالتزام بالمواعيد المحددة أعلاه لضمان انسيابية العمل وجاهزية القاعة للمناسبات اللاحقة.
                          </div>

                          <p style="font-size: 13px; line-height: 1.7; color: #8EA3BF; text-align: center; margin: 0;">
                            بارك الله لكم وجمع بينكم في خير، ونسأل الله أن يجعل هذا المكان صرحاً للبر والخير والبركة.<br><br>
                            <span style="color: #D4AF85; font-size: 14px; font-weight: 800;">جامع الحق — الموالح الجنوبية، ولاية السيب</span>
                          </p>
                        </td>
                      </tr>

                      <!-- Footer (modeled after Re-Sapori) -->
                      <tr>
                        <td align="center" style="padding: 22px 30px; background-color: #060D1A; border-top: 1px solid #1E3A63; font-size: 11px; color: #8EA3BF; line-height: 1.6;">
                          <div style="color: #D4AF85; font-weight: 700; margin-bottom: 4px;">جامع الحق • الصرح الإيماني والمنارة المجتمعية</div>
                          <div>المنطقة السابعة — الموالح الجنوبية — ولاية السيب، سلطنة عمان</div>
                          <div style="margin-top: 6px;">لأي استفسار يرجى مراسلتنا: <a href="mailto:%s" style="color: #FFE8B8; text-decoration: none;">%s</a></div>
                          <div style="margin-top: 8px; font-size: 10px; color: #52637A;">&copy; 2026 جامع الحق. جميع الحقوق محفوظة.</div>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                statusBadgeText,
                greetingHeader,
                greetingParagraph,
                bookingCode,
                occasionTitle,
                notesRow,
                hallName,
                dateDisplay,
                timeDisplay,
                customerName,
                phoneStr,
                totalAmountStr,
                currencyStr,
                replyToEmail,
                replyToEmail
        );
    }

    /**
     * Builds luxury HTML email for password reset OTP.
     */
    private String buildLuxuryOtpHtml(String otpCode) {
        String safeOtp = escapeHtml(otpCode);
        return """
            <!DOCTYPE html>
            <html lang="ar" dir="rtl">
            <head>
              <meta charset="UTF-8">
              <title>رمز التحقق - جامع الحق</title>
            </head>
            <body style="margin: 0; padding: 0; background-color: #050A14; font-family: -apple-system, sans-serif; direction: rtl; text-align: right;">
              <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color: #050A14; padding: 40px 15px;">
                <tr>
                  <td align="center">
                    <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width: 550px; background-color: #0A1628; border: 1px solid #1E3A63; border-radius: 16px; overflow: hidden;">
                      <tr>
                        <td align="center" style="padding: 30px; background: #0D1A2D; border-bottom: 1px solid rgba(212, 175, 133, 0.3);">
                          <div style="font-size: 24px; font-weight: 800; color: #D4AF85;">جامع الحق</div>
                          <div style="font-size: 12px; color: #8EA3BF; margin-top: 4px;">استعادة وتعيين كلمة المرور</div>
                        </td>
                      </tr>
                      <tr>
                        <td style="padding: 30px; color: #FAF7F0;">
                          <p style="font-size: 14px; color: #8EA3BF; text-align: center; margin-bottom: 20px;">
                            لقد تلقينا طلباً لإعادة تعيين كلمة المرور الخاصة بحسابكم على منصة جامع الحق. استخدم رمز التحقق التالي:
                          </p>
                          <div style="background: #050B14; border: 1px dashed #D4AF85; border-radius: 10px; padding: 20px; text-align: center; margin-bottom: 20px;">
                            <div style="font-family: monospace; font-size: 36px; font-weight: 900; letter-spacing: 10px; color: #FFE8B8;">
                              %s
                            </div>
                          </div>
                          <p style="font-size: 12px; color: #E11D48; text-align: center; margin-bottom: 20px;">
                            ⏱️ رمز التحقق صالح لمدة 15 دقيقة فقط للاستخدام لمرة واحدة.
                          </p>
                          <p style="font-size: 11px; color: #8EA3BF; text-align: center; margin: 0;">
                            إذا لم تطلب هذا الرمز، يُرجى تجاهل هذه الرسالة. حسابكم آمن تماماً.
                          </p>
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(safeOtp);
    }

    private String resolveOccasionTitle(String bookingType) {
        if (bookingType == null) return "حجز قاعة";
        String t = bookingType.trim().toLowerCase();
        if (t.contains("3aza") || t.contains("aza") || t.contains("عزاء")) {
            return "مجلس عزاء (3 أيام)";
        }
        if (t.contains("katb") || t.contains("qaran") || t.contains("قران")) {
            return "عقد قران مبارك";
        }
        return "حجز مناسبة";
    }

    private String formatHoursDisplay(Integer startHour, Integer endHour, Integer durationHours) {
        if (startHour == null || endHour == null) {
            return "وفق الفترة المعتمدة";
        }
        String startStr = formatHourTo12h(startHour);
        String endStr = formatHourTo12h(endHour);
        int dur = durationHours != null ? durationHours : Math.max(1, endHour - startHour);
        return "من الساعة " + startStr + " إلى " + endStr + " (" + dur + " ساعات)";
    }

    private String formatHourTo12h(int hour24) {
        int h = hour24 % 24;
        if (h == 0) return "12:00 منتصف الليل";
        if (h == 12) return "12:00 ظهراً";
        if (h < 12) return h + ":00 صباحاً";
        return (h - 12) + ":00 مساءً";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
