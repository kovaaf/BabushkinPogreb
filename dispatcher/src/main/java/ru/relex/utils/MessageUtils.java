package ru.relex.utils;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

// Сеттер(chatId, text)  для объектов SendMessage, чтобы не писать его с нуля в каждом обработчике сообщений
@Component
public class MessageUtils {
	public SendMessage generateSendMessageWithText(Update update, String text) {
		var sendMessage = new SendMessage();
		sendMessage.setChatId(update.getMessage().getChatId());
		sendMessage.setText(text);
		return sendMessage;
	}
}
