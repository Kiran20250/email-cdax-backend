package com.example.cdaxVideo.Controller;
// imports you need
import com.example.cdaxVideo.Entity.User;
import com.example.cdaxVideo.Service.AuthService;
import com.example.cdaxVideo.Service.EmailService;
import com.example.cdaxVideo.Service.JWTService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/2fa")
@CrossOrigin(origins = "*")
public class TwoFactorController {

    @Value("${app.frontend.confirmationBaseUrl:http://192.168.0.119:8080}")
    private String confirmationBaseUrl;
    @Autowired private AuthService authService;
    @Autowired private EmailService emailService;
    @Autowired private JWTService jwtService;



    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> send(
            @RequestParam String email,
            @RequestParam String password) {

        Map<String, Object> resp = new HashMap<>();

        // Step 1: Check if user exists
        var user = authService.getUserByEmail(email);
        if (user == null) {
            resp.put("status", "error");
            resp.put("message", "User not found");
            return ResponseEntity.status(404).body(resp);
        }

        // Step 2: Validate password using existing AuthService logic
        User u = new User();
        u.setEmail(email);
        u.setPassword(password);

        String result = authService.loginUser(u);
        if (!"Login successful".equals(result)) {
            resp.put("status", "failed");
            resp.put("message", result);
            return ResponseEntity.status(401).body(resp);
        }

        // Step 3: Generate token + send email link
        String token = UUID.randomUUID().toString();
        authService.savePendingLoginToken(email, token);

        String url = confirmationBaseUrl + "/api/2fa/confirm?token=" + token;

        String html = "<h3>Confirm your login</h3>"
                + "<a href='" + url + "' "
                + "style='background-color:#4CAF50;color:white;padding:12px 24px;"
                + "text-decoration:none;border-radius:8px;font-weight:bold;'>✅ Yes, it's me</a>";

        emailService.sendHtmlMail(email, "Confirm Login", html);

        resp.put("status", "success");
        resp.put("message", "Confirmation email sent");
        resp.put("token", token);

        return ResponseEntity.ok(resp);
    }






    // inside your controller class (TwoFactorController or AuthController)
// make sure path matches the link you send in email: /api/2fa/confirm?token=...
    @GetMapping("/confirm")
    public ResponseEntity<String> confirm(@RequestParam String token) {
        String email = authService.getEmailByPendingToken(token);
        if (email == null) {
            String htmlErr = "<!doctype html><html><head><meta charset='utf-8'><title>Invalid token</title></head>"
                    + "<body style='font-family:Arial;text-align:center;padding:40px;'>"
                    + "<h2>❌ Invalid or expired token</h2>"
                    + "<p>Please retry login from the app.</p>"
                    + "</body></html>";
            return ResponseEntity.badRequest()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlErr);
        }

        // Generate JWT and verify login token (existing logic)
        String jwt = jwtService.generateToken(email);
        authService.markLoginTokenVerified(token, jwt);
        authService.clearPendingToken(email);

        // ✅ Simplified success HTML
        String html = "<!doctype html>"
                + "<html><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "<title>Login Confirmed</title>"
                + "<style>"
                + "body{font-family:Arial,Helvetica,sans-serif;background:#f6f9fc;margin:0;padding:0;text-align:center;}"
                + ".box{max-width:500px;margin:80px auto;padding:40px;background:#fff;border-radius:12px;"
                + "box-shadow:0 2px 8px rgba(0,0,0,0.1);}"
                + "h1{color:#2e7d32;margin-bottom:10px;}"
                + "p{color:#444;font-size:16px;}"
                + "</style></head>"
                + "<body>"
                + "<div class='box'>"
                + "<h1>✅ Login Confirmed!</h1>"
                + "<p>Welcome to our <b>CDAX App</b>.</p>"
                + "<p>You may now return to the app to continue.</p>"
                + "</div>"
                + "</body></html>";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(html);
    }



}
