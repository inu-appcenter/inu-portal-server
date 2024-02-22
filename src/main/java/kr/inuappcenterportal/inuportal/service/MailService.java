package kr.inuappcenterportal.inuportal.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class MailService {
    private final JavaMailSender javaMailSender;
    @Async
    public void sendMail(String email, String numbers){
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,true);
            String text = "<h1>INTIP 인증번호</h1>" +
                    "<p> <strong><span style=\"font-size:24px;\">" + numbers + "</span></strong></p>";
            helper.setText(text,true);
            helper.setTo(email);
            helper.setSubject("INTIP 가입 인증번호입니다.");
            helper.setFrom("INTIP");
            javaMailSender.send(mimeMessage);


        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }



}
