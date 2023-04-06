package ru.relex.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.relex.model.RabbitQueue.*;

@Configuration
public class RabbitConfiguration {

	@Bean
	public MessageConverter jsonMessageConverter() {
		// Преобразовывает объекты в json и передаёт rabbitmq,
		// либо при получении апдейтов от rabbitmq преобразовывает их в java объекты
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public Queue textMessageQueue() {
		return new Queue(TEXT_MESSAGE_UPDATE);
	}
	@Bean
	public Queue docMessageQueue() {
		return new Queue(DOC_MESSAGE_UPDATE);
	}
	@Bean
	public Queue photoMessageQueue() {
		return new Queue(PHOTO_MESSAGE_UPDATE);
	}
	@Bean
	public Queue answerMessageQueue() {
		return new Queue(ANSWER_MESSAGE_UPDATE);
	}


}
