package ru.relex.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.relex.dto.MailParams;
import ru.relex.service.MailSenderService;

@Service
public class MailSenderServiceImpl implements MailSenderService {
	private final JavaMailSender javaMailSender;
	@Value("${spring.mail.username}")
	private String emailFrom;
	@Value("${service.activation.uri}")
	private String activationServiceUri;

	public MailSenderServiceImpl(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}

	@Override
	public void send(MailParams mailParams) {
		var subject = "Активация учётной записил";
		var messageBody = getActivationMessageBody(mailParams.getId());
		var emailTo = mailParams.getEmailTo();

		SimpleMailMessage mailMessage = new SimpleMailMessage();
		mailMessage.setFrom(emailFrom);
		mailMessage.setTo(emailTo);
		mailMessage.setSubject(subject);
		mailMessage.setText(messageBody);

		javaMailSender.send(mailMessage);
	}

	private String getActivationMessageBody(String id) {
		var msg = String.format("Для завершения регистрации перейдите по ссылке:\n%s", activationServiceUri);
		return msg.replace("{id}", id);
	}
}