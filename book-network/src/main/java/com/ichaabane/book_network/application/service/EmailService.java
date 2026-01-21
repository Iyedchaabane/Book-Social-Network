package com.ichaabane.book_network.application.service;

import com.ichaabane.book_network.infrastructure.email.EmailTemplateName;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final SpringTemplateEngine templateEngine;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${brevo.api-key}")
    private String brevoApiKey;

    @Value("${brevo.sender-email}")
    private String senderEmail;

    @Value("${brevo.sender-name}")
    private String senderName;

    @Async
    public void sendEmail(
            String to,
            String username,
            EmailTemplateName emailTemplateName,
            String confirmationUrl,
            String activationCode,
            String subject) {
        String templateName;
        if (emailTemplateName == null) {
            templateName = "confirm-email";
        } else {
            templateName = emailTemplateName.getName();
        }

        Map<String, Object> model = new HashMap<>();
        model.put("username", username);
        model.put("confirmationUrl", confirmationUrl);
        model.put("activationCode", activationCode);

        Context context = new Context();
        context.setVariables(model);

        String htmlContent = templateEngine.process(templateName, context);

        // Prepare Brevo API request
        String brevoUrl = "https://api.brevo.com/v3/smtp/email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("api-key", brevoApiKey);

        Map<String, Object> emailRequest = new HashMap<>();
        
        Map<String, String> sender = new HashMap<>();
        sender.put("email", senderEmail);
        sender.put("name", senderName);
        emailRequest.put("sender", sender);

        Map<String, String> recipient = new HashMap<>();
        recipient.put("email", to);
        recipient.put("name", username);
        emailRequest.put("to", new Map[]{recipient});

        emailRequest.put("subject", subject);
        emailRequest.put("htmlContent", htmlContent);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(emailRequest, headers);

        restTemplate.postForEntity(brevoUrl, request, String.class);
    }
}