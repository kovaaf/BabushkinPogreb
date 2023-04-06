package ru.relex.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

// отправка ответов с ноды в брокер
public interface ProducerService {
	void produceAnswer(SendMessage sendMessage);
}
