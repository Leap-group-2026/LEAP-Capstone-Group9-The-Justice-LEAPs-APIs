package main.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired 
    private JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body){
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom("ribbittrade@gmail.com"); 
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}