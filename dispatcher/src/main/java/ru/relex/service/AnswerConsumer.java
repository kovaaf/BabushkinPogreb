package ru.relex.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

// Принимает ответы из rabbot-mq и передаёт в UpdateController
// Имя сервиса-цели указывается с помощью аннотации
public interface AnswerConsumer {
	void consume(SendMessage sendMessage);
}
