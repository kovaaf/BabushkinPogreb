package ru.relex.service;

import org.telegram.telegrambots.meta.api.objects.Update;

// Передаёт апдейты в rabbit-mq
public interface UpdateProducer {
	void produce(String rabbitQueue, Update update);
}
