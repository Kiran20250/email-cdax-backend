package com.example.cdaxVideo.Service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private Environment env;

    /**
     * ✅ Send HTML email via SendGrid
     * @param to recipient email address
     * @param subject subject of the email
     * @param htmlContent HTML message body
     */
    public void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            // 🗝️ Get API Key from environment or application.properties
            String apiKey = env.getProperty("SENDGRID_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new RuntimeException("SENDGRID_API_KEY not found in environment or application.properties");
            }

            // 📨 Get sender email (fallback to default)
            String fromEmail = env.getProperty("app.email.from", "no-reply@cdaxapp.com");

            Email from = new Email(fromEmail);
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toEmail, content);

            // Send request
            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            // ✅ Debug logs
            System.out.println("📨 Email sent to: " + to);
            System.out.println("✅ SendGrid response code: " + response.getStatusCode());
            if (response.getStatusCode() >= 400) {
                System.err.println("⚠️ SendGrid error: " + response.getBody());
            }

        } catch (IOException e) {
            System.err.println("❌ SendGrid IO Exception: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            System.err.println("❌ Email sending failed: " + e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }
}
