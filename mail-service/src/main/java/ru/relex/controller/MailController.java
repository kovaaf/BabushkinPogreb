package ru.relex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.relex.dto.MailParams;
import ru.relex.service.MailSenderService;

@RestController
@RequestMapping("/mail")
public class MailController {
	private final MailSenderService mailSenderService;

	public MailController(MailSenderService mailSenderService) {
		this.mailSenderService = mailSenderService;
	}

	@PostMapping("/send")
	public ResponseEntity<?> sendActivationMail(@RequestBody MailParams mailParams) {
		mailSenderService.send(mailParams);
		// TODO Controller advice - перехват exception и отправка соответствующих ответов
		return ResponseEntity.ok().build();
	}
}
