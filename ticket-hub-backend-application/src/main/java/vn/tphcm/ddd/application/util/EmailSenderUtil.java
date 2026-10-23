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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import vn.tphcm.ddd.infrastructure.mq.email.EmailMQMessage;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "EMAIL-SENDER-UTIL")
public class EmailSenderUtil {

    private final SpringTemplateEngine templateEngine;

    private final JavaMailSender mailSender;

    public void sendTextEmail(EmailMQMessage message) {
        Context context = new Context();

        switch (message.getEventType()) {
            case "VERIFY_EMAIL" -> {
                context.setVariable("verificationCode", message.getContent());

                String htmlContent = templateEngine.process("send-otp", context);

                sendEmailHtml(message.getRecipient(), "Confirm Account", htmlContent);
            }

            case "RESET_PASSWORD" -> {
                context.setVariable("verificationCode", message.getContent());

                String htmlContent = templateEngine.process("reset-password", context);

                sendEmailHtml(message.getRecipient(), "Reset Password", htmlContent);
            }

            case "PURCHASE" -> {
                if (message.getData() == null) {
                    throw new IllegalArgumentException("Data is null");
                }
                message.getData().forEach(context::setVariable);

                String htmlContent = templateEngine.process("ticket-purchase-success", context);
                sendEmailHtml(message.getRecipient(), "Purchase", htmlContent);
            }

            default -> sendEmailHtml(message.getRecipient(), "Error", message.getContent());

        }
    }

    private void sendEmailHtml(String to, String subject, String context) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(context, true);

            mailSender.send(mimeMessage);

        } catch (MessagingException e) {
            log.error("Email sending failed {}, {}", e, e.getMessage());
        }
    }
}
