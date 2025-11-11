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

    public void sendHtmlMail(String to, String subject, String htmlContent) {
        try {
            String apiKey = env.getProperty("SENDGRID_API_KEY");
            if (apiKey == null) {
                throw new RuntimeException("SENDGRID_API_KEY not found in environment");
            }

            Email from = new Email(env.getProperty("app.email.from"));
            Email toEmail = new Email(to);
            Content content = new Content("text/html", htmlContent);
            Mail mail = new Mail(from, subject, toEmail, content);

            SendGrid sg = new SendGrid(apiKey);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);

            System.out.println("📨 SendGrid response code: " + response.getStatusCode());
            System.out.println("Response body: " + response.getBody());

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to send email via SendGrid: " + e.getMessage());
        }
    }
}
