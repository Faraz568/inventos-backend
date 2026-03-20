package com.inventos.service;

import com.inventos.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OtpService {

    @Value("${app.otp.expiry-minutes:10}")
    private int expiryMinutes;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${resend.from.address:onboarding@resend.dev}")
    private String fromAddress;

    private record OtpEntry(String otp, Instant expiresAt) {}

    private final Map<String, OtpEntry> emailStore = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> resetStore  = new ConcurrentHashMap<>();
    private final Map<String, OtpEntry> loginStore  = new ConcurrentHashMap<>();

    public void sendOtp(String email) {
        String otp = generateOtp();
        emailStore.put(email, new OtpEntry(otp, expiry()));
        sendHtml(email, "InventOS — Email Verification Code",
            buildEmailHtml("Email Verification",
                "Use the code below to verify your email address.", otp,
                "This code expires in " + expiryMinutes + " minutes."));
    }

    public boolean verifyOtp(String email, String otp) { return check(emailStore, email, otp); }

    public void sendLoginOtp(String email, String username) {
        String otp = generateOtp();
        loginStore.put(username, new OtpEntry(otp, expiry()));
        sendHtml(email, "InventOS — Sign-in Verification Code",
            buildEmailHtml("Sign-in Verification",
                "A sign-in attempt was made for your account. Use the code below to complete sign-in.", otp,
                "This code expires in " + expiryMinutes + " minutes."));
    }

    public boolean verifyLoginOtp(String username, String otp) { return check(loginStore, username, otp); }

    public void sendPasswordResetOtp(String email) {
        String otp = generateOtp();
        resetStore.put(email, new OtpEntry(otp, expiry()));
        sendHtml(email, "InventOS — Password Reset Code",
            buildEmailHtml("Password Reset",
                "A password reset was requested for your account. Use the code below.", otp,
                "This code expires in " + expiryMinutes + " minutes."));
    }

    public boolean verifyPasswordResetOtp(String email, String otp) { return check(resetStore, email, otp); }

    private void sendHtml(String to, String subject, String htmlBody) {
        try {
            String escapedHtml = htmlBody
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");

            String payload = "{\"from\":\"" + fromAddress + "\","
                + "\"to\":[\"" + to + "\"],"
                + "\"subject\":\"" + subject + "\","
                + "\"html\":\"" + escapedHtml + "\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.resend.com/emails"))
                .header("Authorization", "Bearer " + resendApiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent via Resend to {} — {}", to, subject);
            } else {
                log.error("Resend API error {}: {}", response.statusCode(), response.body());
                throw new BusinessException("Failed to send email. Please try again.");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new BusinessException("Failed to send email. Please try again.");
        }
    }

    private String buildEmailHtml(String title, String subtitle, String otp, String footer) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f0f1f3;padding:40px'>"
            + "<div style='max-width:480px;margin:auto;background:#fff;border-radius:12px;border:1px solid #e2e4e8;overflow:hidden'>"
            + "<div style='background:linear-gradient(135deg,#00c9b1,#00d97e);padding:24px 32px'>"
            + "<div style='font-size:20px;font-weight:700;color:#fff'>InventOS</div>"
            + "<div style='color:rgba(255,255,255,.75);font-size:12px'>Inventory Management System</div></div>"
            + "<div style='padding:32px'>"
            + "<h2 style='margin:0 0 8px;font-size:18px;color:#1a1d23'>" + title + "</h2>"
            + "<p style='margin:0 0 28px;font-size:13px;color:#4b5563'>" + subtitle + "</p>"
            + "<div style='background:#f0fdf4;border:2px solid #00c9b1;border-radius:10px;padding:20px;text-align:center;margin-bottom:24px'>"
            + "<div style='font-size:11px;color:#00a896;letter-spacing:.1em;text-transform:uppercase;margin-bottom:8px'>Verification Code</div>"
            + "<div style='font-size:36px;font-weight:700;color:#1a1d23;letter-spacing:.25em'>" + otp + "</div></div>"
            + "<p style='font-size:12px;color:#9ca3af'>" + footer + "</p></div>"
            + "<div style='background:#f7f8fa;border-top:1px solid #e2e4e8;padding:16px 32px;text-align:center'>"
            + "<p style='font-size:11px;color:#9ca3af;margin:0'>Do not share this code with anyone.</p></div>"
            + "</div></body></html>";
    }

    private String generateOtp() { return String.format("%06d", new Random().nextInt(999999)); }
    private Instant expiry() { return Instant.now().plusSeconds(expiryMinutes * 60L); }
    private boolean check(Map<String, OtpEntry> store, String key, String otp) {
        OtpEntry entry = store.get(key);
        if (entry == null) return false;
        if (Instant.now().isAfter(entry.expiresAt())) { store.remove(key); return false; }
        if (entry.otp().equals(otp)) { store.remove(key); return true; }
        return false;
    }
}