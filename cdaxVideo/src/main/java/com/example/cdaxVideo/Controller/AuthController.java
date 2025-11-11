package com.example.cdaxVideo.Controller;


import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Service.AuthService;
import com.example.cdaxVideo.Service.EmailService;
import com.example.cdaxVideo.Service.JWTService;
import com.example.cdaxVideo.Service.OTPService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private AuthService authService;
    @Autowired private OTPService otpService;
    @Autowired private EmailService emailService;
    @Autowired private JWTService jwtService;

    // ---------------- Existing APIs ----------------

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        // Before register, check if email verified
        boolean verified = otpService.isEmailVerified(user.getEmail());
        if (!verified) {
            return ResponseEntity.badRequest().body("Please verify your email before registration");
        }

        String result = authService.registerUser(user);
        switch (result) {
            case "Email already exists":
            case "Passwords do not match":
            case "Mobile number already registered":
                return ResponseEntity.badRequest().body(result);
            default:
                otpService.clearEmailVerification(user.getEmail());
                return ResponseEntity.ok(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody User user) {
        String result = authService.loginUser(user);
        switch (result) {
            case "Login successful":
                return ResponseEntity.ok(result);
            case "Incorrect password":
                return ResponseEntity.status(401).body(result);
            case "Email not found":
                return ResponseEntity.status(404).body(result);
            default:
                return ResponseEntity.badRequest().body("Something went wrong");
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testServer() {
        return ResponseEntity.ok("Server is running!");
    }

    @GetMapping("/firstName")
    public Map<String, Object> getFirstName(@RequestParam String email) {
        String firstName = authService.getFirstNameByEmail(email);
        Map<String, Object> resp = new HashMap<>();
        if (firstName != null) {
            resp.put("status", "success");
            resp.put("firstName", firstName);
        } else {
            resp.put("status", "error");
            resp.put("message", "User not found");
        }
        return resp;
    }

    @GetMapping("/getUserByEmail")
    public ResponseEntity<Map<String, Object>> getUserByEmail(@RequestParam String email) {
        User user = authService.getUserByEmail(email);
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("status", "success");
            response.put("email", user.getEmail());
            response.put("firstName", user.getFirstName());
            response.put("lastName", user.getLastName());
            response.put("mobile", user.getMobile());
            response.put("subscribed", user.isSubscribed());
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "error");
            response.put("message", "User not found");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PostMapping("/toggleSubscription")
    public ResponseEntity<Map<String, Object>> toggleSubscription(@RequestParam String email) {
        boolean updated = authService.toggleSubscription(email);
        Map<String, Object> response = new HashMap<>();
        if (updated) {
            response.put("status", "success");
            response.put("message", "Subscription status updated");
        } else {
            response.put("status", "error");
            response.put("message", "User not found");
        }
        return ResponseEntity.ok(response);
    }

    // ---------------- Email OTP Verification (Registration) ----------------

    @PostMapping("/sendEmailOtp")
    public ResponseEntity<Map<String, String>> sendEmailOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        Map<String, String> resp = new HashMap<>();

        boolean alreadyExists = authService.checkEmailExists(email);
        if (alreadyExists) {
            resp.put("status", "error");
            resp.put("message", "Email already registered");
            return ResponseEntity.badRequest().body(resp);
        }

        otpService.generateAndSendEmailOtp(email);
        resp.put("status", "ok");
        resp.put("message", "OTP sent to email");
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/verifyEmailOtp")
    public ResponseEntity<Map<String, String>> verifyEmailOtp(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String otp = req.get("otp");

        boolean ok = otpService.verifyEmailOtp(email, otp);
        Map<String, String> resp = new HashMap<>();

        if (ok) {
            resp.put("status", "verified");
            resp.put("message", "Email verified successfully");
        } else {
            resp.put("status", "invalid");
            resp.put("message", "Invalid or expired OTP");
        }
        return ResponseEntity.ok(resp);
    }

    // ---------------- Login 2FA (Email confirmation link) ----------------

    @PostMapping("/initiate-login-2fa")
    public ResponseEntity<Map<String, String>> initiateLogin2FA(@RequestBody Map<String, String> req) {
        String email = req.get("email");
        String password = req.get("password");
        Map<String, String> resp = new HashMap<>();

        User u = new User();
        u.setEmail(email);
        u.setPassword(password);
        String res = authService.loginUser(u);

        if ("Login successful".equals(res)) {
            String token = UUID.randomUUID().toString();
            authService.savePendingLoginToken(email, token);

            String base = System.getProperty("app.frontend.confirmationBaseUrl");
            if (base == null) base = "http://192.168.1.5:8080"; // 👈 apne PC ka local IP address yahan daal
            String link = base + "/api/auth/confirm-2fa?token=" + token;

            String html = "<h3>Confirm your login</h3>"
                    + "<p>If you initiated this login, click below:</p>"
                    + "<a href='" + link + "' "
                    + "style='background-color:#4CAF50;color:white;padding:12px 24px;"
                    + "text-decoration:none;border-radius:8px;font-weight:bold;'>✅ Yes, it's me</a>"
                    + "<p>If this wasn't you, ignore this email.</p>";

            emailService.sendHtmlMail(email, "Login Confirmation", html);

            resp.put("status", "pending");
            resp.put("message", "Confirmation email sent");
            return ResponseEntity.ok(resp);
        } else if ("Incorrect password".equals(res)) {
            resp.put("status", "failed");
            resp.put("message", "Incorrect password");
            return ResponseEntity.status(401).body(resp);
        } else {
            resp.put("status", "failed");
            resp.put("message", res);
            return ResponseEntity.badRequest().body(resp);
        }
    }

    @GetMapping("/confirm-2fa")
    public ResponseEntity<Map<String, String>> confirm2FA(@RequestParam String token) {
        Map<String, String> resp = new HashMap<>();
        String email = authService.getEmailByPendingToken(token);
        if (email == null) {
            resp.put("status", "error");
            resp.put("message", "Invalid or expired token");
            return ResponseEntity.badRequest().body(resp);
        }

        String jwt = jwtService.generateToken(email);
        authService.markLoginTokenVerified(token, jwt);
        authService.clearPendingToken(email);

        resp.put("status", "confirmed");
        resp.put("jwt", jwt);
        resp.put("message", "Login confirmed");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/login-status")
    public ResponseEntity<Map<String, String>> loginStatus(@RequestParam String email) {
        Map<String, String> resp = new HashMap<>();
        try {
            var user = authService.getUserByEmail(email);
            if (user == null) {
                resp.put("status", "error");
                resp.put("message", "User not found");
                return ResponseEntity.status(404).body(resp);
            }
            var maybe = authService.findLatestLoginTokenForUser(user.getId());
            if (maybe.isPresent() && maybe.get().isVerified() && maybe.get().getJwt() != null) {
                resp.put("status", "confirmed");
                resp.put("jwt", maybe.get().getJwt());
                return ResponseEntity.ok(resp);
            } else {
                resp.put("status", "pending");
                return ResponseEntity.ok(resp);
            }
        } catch (Exception e) {
            resp.put("status", "error");
            resp.put("message", "Server error: " + e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }
}
