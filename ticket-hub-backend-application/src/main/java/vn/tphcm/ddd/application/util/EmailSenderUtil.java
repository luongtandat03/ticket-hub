/*
 * @ (#) EmailSenderUtil.java       1.0     9/21/2026
 *
 * Copyright (c) 2026. All rights reserved.
 */

package vn.tphcm.ddd.application.util;
/*
 * @author: Luong Tan Dat
 * @date: 9/21/2026
 */

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SENDER-UTIL")
public class EmailSenderUtil {

    private static final String EMAIL_HOST = "";

    private final JavaMailSender mailSender;

    public void sendTextEmail(String to, String subject, String context) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(to);
        message.setSubject(subject);
        message.setText(context);
        message.setFrom(EMAIL_HOST);
        try {
            mailSender.send(message);
            log.info("Email sent successfully");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendEmailHtml(String to, String subject, String context) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(EMAIL_HOST);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(context, true);

            mailSender.send(mimeMessage);

            log.info("Email sent successfully");
        } catch (MessagingException e) {
            log.error("Email sending failed " + e.getMessage(), e);
        }
    }
}
