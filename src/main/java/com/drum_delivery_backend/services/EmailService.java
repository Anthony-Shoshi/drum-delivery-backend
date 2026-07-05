package com.drum_delivery_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    
    @Value("${app.email.from}")
    private String fromEmail;
    
    @Value("${app.email.from-name}")
    private String fromName;
    
    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }
    
    /**
     * Send a simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            
            mailSender.send(message);
            logger.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send an HTML email using a template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            // Set email properties
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);
            
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            logger.info("HTML email sent successfully to: {} using template: {}", to, templateName);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to: {} using template: {}", to, templateName, e);
            throw new RuntimeException("Failed to send HTML email", e);
        } catch (Exception e) {
            logger.error("Unexpected error sending HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    /**
     * Send 2FA verification code email
     */
    public void send2FACode(String to, String userName, String code) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "code", code,
            "fromName", fromName
        );
        
        sendHtmlEmail(to, "Your verification code - Drum Delivery System", "2fa-code", variables);
    }
    
    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String userName, String resetToken) {
        // For now, we'll send the token directly. In production, this should be a link
        // Example: https://yourdomain.com/reset-password?token=resetToken
        String resetLink = "Please use this reset token: " + resetToken;
        
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "resetToken", resetToken,
            "resetLink", resetLink,
            "fromName", fromName
        );
        
        sendHtmlEmail(to, "Password Reset Request - Drum Delivery System", "password-reset", variables);
    }
    
    /**
     * Send password reset confirmation email
     */
    public void sendPasswordResetConfirmation(String to, String userName) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "fromName", fromName
        );
        
        sendHtmlEmail(to, "Password Reset Successful - Drum Delivery System", "password-reset-confirmation", variables);
    }
    
    /**
     * Send welcome email to new users
     */
    public void sendWelcomeEmail(String to, String userName, String temporaryPassword) {
        Map<String, Object> variables = Map.of(
            "userName", userName,
            "temporaryPassword", temporaryPassword,
            "fromName", fromName
        );
        
        sendHtmlEmail(to, "Welcome to Drum Delivery System", "welcome", variables);
    }
    
    /**
     * Test email connectivity
     */
    public boolean testEmailConnection() {
        try {
            sendSimpleEmail(fromEmail, "Email Service Test", "This is a test email to verify email service configuration.");
            return true;
        } catch (Exception e) {
            logger.error("Email connection test failed", e);
            return false;
        }
    }
}