package ru.relex.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.relex.service.UpdateProducer;

// Реализуем программирование через интерфейсы т.к. потребуется в тестировании заменить отдельный сервисы моками
// Так будет проще, не нужно поднимать rabbit-mq для тестирования
@Service
@Log4j
public class UpdateProducerImpl implements UpdateProducer {
	private final RabbitTemplate rabbitTemplate;

	public UpdateProducerImpl(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	@Override
	public void produce(String rabbitQueue, Update update) {
		log.debug(update.getMessage().getText());
		rabbitTemplate.convertAndSend(rabbitQueue, update);
	}
}
