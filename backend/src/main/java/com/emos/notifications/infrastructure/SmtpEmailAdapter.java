package com.emos.notifications.infrastructure;

import com.emos.notifications.application.EmailPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.*;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "emos.email.enabled", havingValue = "true")
public class SmtpEmailAdapter implements EmailPort {
  private final JavaMailSender sender;

  public SmtpEmailAdapter(JavaMailSender sender) {
    this.sender = sender;
  }

  public void send(String to, String subject, String text, String html) {
    var message = sender.createMimeMessage();
    try {
      var helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setTo(to);
      helper.setSubject(subject);
      helper.setText(text, html);
      sender.send(message);
    } catch (jakarta.mail.MessagingException e) {
      throw new IllegalStateException("Email composition failed", e);
    }
  }
}
