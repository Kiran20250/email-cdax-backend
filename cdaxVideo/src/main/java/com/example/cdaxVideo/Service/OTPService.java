package com.example.cdaxVideo.Service;

import com.example.cdaxVideo.Entity.OtpVerification;
import com.example.cdaxVideo.Repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private OtpVerificationRepository otpRepo;

    @Autowired
    private EmailService emailService;

    //  6-digit random OTP generate
    private String generateOtp() {
        int otp = new Random().nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    // Generate + send OTP via email
    public String generateAndSendEmailOtp(String email) {
        try {
            // Delete any old OTPs for same email
            otpRepo.deleteAllByTargetAndType(email, "EMAIL");

            String otp = generateOtp();
            OtpVerification entity = new OtpVerification();
            entity.setTarget(email);
            entity.setOtp(otp);
            entity.setType("EMAIL");
            entity.setExpiresAt(LocalDateTime.now().plusMinutes(10)); // 10 min valid
            entity.setVerified(false);
            otpRepo.save(entity);

            // Send mail
            String html = "<h3>Your CDAX verification code:</h3>"
                    + "<p style='font-size:18px;font-weight:bold;'>" + otp + "</p>"
                    + "<p>This code will expire in <b>10 minutes</b>.</p>";
            emailService.sendHtmlMail(email, "CDAX Verification Code", html);

            System.out.println(" OTP sent successfully to " + email + " | OTP: " + otp);
            return otp;

        } catch (Exception e) {
            System.err.println("❌ Failed to send OTP: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to generate/send OTP");
        }
    }

    //  Verify OTP
    public boolean verifyOtp(String target, String type, String otp) {
        Optional<OtpVerification> maybeOtp = otpRepo.findTopByTargetAndTypeOrderByIdDesc(target, type);

        if (maybeOtp.isEmpty()) return false;

        OtpVerification record = maybeOtp.get();

        // Conditions
        if (record.isVerified()) return false;
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) return false;
        if (!record.getOtp().trim().equals(otp.trim())) return false;

        // Mark verified
        record.setVerified(true);
        otpRepo.save(record);

        System.out.println(" OTP verified successfully for: " + target);
        return true;
    }

    //  Shortcut for email OTP verification
    public boolean verifyEmailOtp(String email, String otp) {
        return verifyOtp(email, "EMAIL", otp);
    }

    //  Check if email already verified (used in register)
    public boolean isEmailVerified(String email) {
        return otpRepo.findTopByTargetAndTypeOrderByIdDesc(email, "EMAIL")
                .map(OtpVerification::isVerified)
                .orElse(false);
    }

    //  Clear verification status (after successful register)
    public void clearEmailVerification(String email) {
        otpRepo.findTopByTargetAndTypeOrderByIdDesc(email, "EMAIL")
                .ifPresent(rec -> {
                    rec.setVerified(false);
                    otpRepo.save(rec);
                });
    }

    //  Resend OTP if needed
    public String resendOtp(String email) {
        System.out.println("♻️ Resending OTP to: " + email);
        return generateAndSendEmailOtp(email);
    }
}

