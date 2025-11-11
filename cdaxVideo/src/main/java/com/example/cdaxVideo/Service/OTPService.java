package com.example.cdaxVideo.Service;


import com.example.cdaxVideo.Entity.OtpVerification;
import com.example.cdaxVideo.Repository.OtpVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private OtpVerificationRepository otpRepo;

    @Autowired
    private EmailService emailService;

    private String generateOtp() {
        int n = new Random().nextInt(900000) + 100000;
        return String.valueOf(n);
    }

    public String generateAndSendEmailOtp(String email) {
        otpRepo.deleteAllByTargetAndType(email, "EMAIL"); // clear old
        String otp = generateOtp();
        OtpVerification rec = new OtpVerification();
        rec.setTarget(email);
        rec.setOtp(otp);
        rec.setType("EMAIL");
        rec.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        rec.setVerified(false);
        otpRepo.save(rec);

        String html = "<p>Your CDAX verification code is <b>" + otp + "</b>. It expires in 10 minutes.</p>";
        emailService.sendHtmlMail(email, "CDAX Verification Code", html);
        return otp;
    }


    public boolean verifyOtp(String target, String type, String otp) {
        return otpRepo.findTopByTargetAndTypeOrderByIdDesc(target, type)
                .map(rec -> {
                    if (rec.isVerified()) return false;
                    if (rec.getExpiresAt().isBefore(LocalDateTime.now())) return false;
                    if (!rec.getOtp().trim().equals(otp.trim())) return false;
                    rec.setVerified(true);
                    otpRepo.save(rec);
                    System.out.println("Expected OTP: " + rec.getOtp() + " | Provided: " + otp);
                    System.out.println("Expired: " + rec.getExpiresAt().isBefore(LocalDateTime.now()));
                    System.out.println("Already verified: " + rec.isVerified());

                    return true;
                }).orElse(false);
    }

    // ✅ check if email verified
    public boolean isEmailVerified(String email) {
        return otpRepo.findTopByTargetAndTypeOrderByIdDesc(email, "EMAIL")
                .map(OtpVerification::isVerified)
                .orElse(false);
    }

    // ✅ clear email verification after registration done
    public void clearEmailVerification(String email) {
        otpRepo.findTopByTargetAndTypeOrderByIdDesc(email, "EMAIL")
                .ifPresent(rec -> {
                    rec.setVerified(false);
                    otpRepo.save(rec);
                });
    }

    // ✅ ADD THIS missing method used in AuthController
    public boolean verifyEmailOtp(String email, String otp) {
        return verifyOtp(email, "EMAIL", otp);
    }
}
