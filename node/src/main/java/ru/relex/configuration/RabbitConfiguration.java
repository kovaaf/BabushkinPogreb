package ru.relex.configuration;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfiguration {

	@Bean
	public MessageConverter jsonMessageConverter() {
		// Преобразовывает объекты в json и передаёт кролику,
		// либо при получении апдейтов от кролика преобразовывает их в джава объекты
		return new Jackson2JsonMessageConverter();
	}

}
