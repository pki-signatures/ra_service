package com.ivan.ra.service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

@Component
public class EmailService {

  @Value("${mail.smtp.host}")
  private String smtpHost;

  @Value("${mail.smtp.port}")
  private String smtpPort;

  @Value("${mail.smtp.auth}")
  private String smtpAuth;

  @Value("${mail.smtp.starttls.enable}")
  private String smtpStartTlsEnable;

  @Value("${from.email.address}")
  private String fromEmailAddress;

  @Value("${email.username}")
  private String username;

  @Value("${email.password}")
  private String password;

  @Value("${cert.register.email.subject}")
  private String certRegisterEmailSubject;

  @Value("${cert.register.success.email.subject}")
  private String certRegisterSuccessEmailSubject;

  @Value("${cert.register.email.pin}")
  private String certRegisterEmailPin;

  public void sendCertRegisterEmail(String toEmailAddress, String body) throws Exception {
      sendEmail(toEmailAddress, certRegisterEmailSubject, body);
  }

  public void sendCertRegisterSuccessEmail(String toEmailAddress, String body) throws Exception {
    sendEmail(toEmailAddress, certRegisterSuccessEmailSubject, body);
  }

  public void sendCertRegisterPinEmail(String toEmailAddress, String body) throws Exception {
    sendEmail(toEmailAddress, certRegisterEmailPin, body);
  }

  private void sendEmail(String toEmailAddress, String subject, String body) throws Exception {
    Properties props = new Properties();
    props.put("mail.smtp.host", smtpHost);
    props.put("mail.smtp.port", smtpPort);
    props.put("mail.smtp.auth", smtpAuth);
    props.put("mail.smtp.starttls.enable", smtpStartTlsEnable);

    Authenticator auth = new Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    };
    Session session = Session.getInstance(props, auth);

    MimeMessage msg = new MimeMessage(session);
    msg.addHeader("Content-type", "text/html; charset=UTF-8");

    msg.setFrom(new InternetAddress(fromEmailAddress));
    msg.setSubject(subject, "UTF-8");
    msg.setContent(body, "text/html");
    msg.setSentDate(new Date());
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmailAddress, false));
    Transport.send(msg);
  }
}
