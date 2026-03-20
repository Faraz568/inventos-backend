package com.inventos.service;

import com.inventos.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final JavaMailSender mailSender;

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${app.mail.from}")
    private String fromAddress;

    private record OtpEntry(String otp, Instant expiresAt) {}

    
    private final Map<String, OtpEntry> emailStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> resetStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> loginStore = new ConcurrentHashMap<>();  

    

    public void sendOtp(String email) {
        String otp = generateOtp();
        emailStore.put(email, new OtpEntry(otp, expiry()));
        sendHtml(email,
            "InventOS — Email Verification Code",
            buildEmailHtml(
                "Email Verification",
                "Use the code below to verify your email address.",
                otp,
                "This code expires in " + expiryMinutes + " minutes."
            ));
    }

    public boolean verifyOtp(String email, String otp) {
        return check(emailStore, email, otp);
    }

    

    public void sendLoginOtp(String email, String username) {
        String otp = generateOtp();
        loginStore.put(username, new OtpEntry(otp, expiry()));
        sendHtml(email,
            "InventOS — Sign-in Verification Code",
            buildEmailHtml(
                "Sign-in Verification",
                "A sign-in attempt was made for your InventOS account. Use the code below to complete sign-in.",
                otp,
                "This code expires in " + expiryMinutes + " minutes. If you did not attempt to sign in, please change your password immediately."
            ));
    }

    public boolean verifyLoginOtp(String username, String otp) {
        return check(loginStore, username, otp);
    }

    

    public void sendPasswordResetOtp(String email) {
        String otp = generateOtp();
        resetStore.put(email, new OtpEntry(otp, expiry()));
        sendHtml(email,
            "InventOS — Password Reset Code",
            buildEmailHtml(
                "Password Reset",
                "A password reset was requested for your InventOS account. Use the code below to reset your password.",
                otp,
                "This code expires in " + expiryMinutes + " minutes. If you did not request this, your account is safe — ignore this email."
            ));
    }

    public boolean verifyPasswordResetOtp(String email, String otp) {
        return check(resetStore, email, otp);
    }

    

    private String buildEmailHtml(String title, String subtitle, String otp, String footer) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#f0f1f3;font-family:'Inter',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 16px;">
                <tr><td align="center">
                  <table width="480" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:12px;border:1px solid #e2e4e8;overflow:hidden;">

                    <!-- Header -->
                    <tr>
                      <td style="background:linear-gradient(135deg,#00c9b1,#00d97e);padding:24px 32px;">
                        <div style="font-family:'JetBrains Mono',monospace;font-size:20px;font-weight:700;color:#fff;letter-spacing:.04em;">
                          InventOS
                        </div>
                        <div style="color:rgba(255,255,255,.75);font-size:12px;margin-top:3px;">
                          Inventory Management System
                        </div>
                      </td>
                    </tr>

                    <!-- Body -->
                    <tr>
                      <td style="padding:32px;">
                        <h2 style="margin:0 0 8px;font-size:18px;font-weight:600;color:#1a1d23;letter-spacing:-.01em;">
                          %s
                        </h2>
                        <p style="margin:0 0 28px;font-size:13.5px;color:#4b5563;line-height:1.6;">
                          %s
                        </p>

                        <!-- OTP box -->
                        <div style="background:#f0fdf4;border:2px solid #00c9b1;border-radius:10px;padding:20px;text-align:center;margin-bottom:24px;">
                          <div style="font-size:11px;font-weight:600;color:#00a896;letter-spacing:.1em;text-transform:uppercase;margin-bottom:8px;">
                            Verification Code
                          </div>
                          <div style="font-family:'JetBrains Mono',monospace;font-size:36px;font-weight:700;color:#1a1d23;letter-spacing:.25em;">
                            %s
                          </div>
                        </div>

                        <p style="margin:0;font-size:12.5px;color:#9ca3af;line-height:1.6;">
                          %s
                        </p>
                      </td>
                    </tr>

                    <!-- Footer -->
                    <tr>
                      <td style="background:#f7f8fa;border-top:1px solid #e2e4e8;padding:16px 32px;">
                        <p style="margin:0;font-size:11.5px;color:#9ca3af;text-align:center;">
                          This email was sent by InventOS. Do not share this code with anyone.
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(title, title, subtitle, otp, footer);
    }

    

    private String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    private Instant expiry() {
        return Instant.now().plusSeconds(expiryMinutes * 60L);
    }

    private boolean check(Map<String, OtpEntry> store, String key, String otp) {
        OtpEntry entry = store.get(key);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) { store.remove(key); return false; }
        if (entry.otp().equals(otp)) { store.remove(key); return true; }
        return false;
    }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);  
            mailSender.send(msg);
            log.info("HTML email sent to {} — {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new BusinessException("Failed to send email. Please try again.");
        }
    }
}
