package ru.relex.service.impl;

import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.relex.dao.AppUserDao;
import ru.relex.dao.RawDataDAO;
import ru.relex.entity.AppDocument;
import ru.relex.entity.AppPhoto;
import ru.relex.entity.AppUser;
import ru.relex.entity.RawData;
import ru.relex.exceptions.UploadFileException;
import ru.relex.service.AppUserService;
import ru.relex.service.FileService;
import ru.relex.service.MainService;
import ru.relex.service.ProducerService;
import ru.relex.service.enums.LinkType;
import ru.relex.service.enums.ServiceCommand;

import static ru.relex.entity.enums.UserState.BASIC_STATE;
import static ru.relex.entity.enums.UserState.WAIT_FOR_EMAIL_STATE;
import static ru.relex.service.enums.ServiceCommand.*;

@Service
@Log4j
public class MainServiceImpl implements MainService {
	private final RawDataDAO rawDataDAO;
	private final ProducerService producerService;
	private final AppUserDao appUserDao;
	private final FileService fileService;
	private final AppUserService appUserService;

	public MainServiceImpl(RawDataDAO rawDataDAO, ProducerService producerService, AppUserDao appUserDao,
	                       FileService fileService, AppUserService appUserService) {
		this.rawDataDAO = rawDataDAO;
		this.producerService = producerService;
		this.appUserDao = appUserDao;
		this.fileService = fileService;
		this.appUserService = appUserService;
	}

	@Override
	public void processTextMessage(Update update) {
		saveRawData(update);
		var appUser = findOrSaveAppUser(update);
		var userState = appUser.getUserState();
		var text = update.getMessage().getText();
		var output = "";

		var serviceCommand = ServiceCommand.fromValue(text);
		if (CANCEL.equals(serviceCommand)) {
			output = cancelProcess(appUser);
		} else if (BASIC_STATE.equals(userState)) {
			output = processServiceCommand(appUser, text);
		} else if (WAIT_FOR_EMAIL_STATE.equals(userState)) {
			output = appUserService.setEmail(appUser, text);
		} else {
			log.error("Unknown user state: " + userState);
			output = "Неизвестная ошибка! Введите /cancel и попробуйте снова!";
		}

		var chatId = update.getMessage().getChatId();
		sendAnswer(output, chatId);
	}

	@Override
	public void processDocMessage(Update update) {
		saveRawData(update);
		var appUser = findOrSaveAppUser(update);
		var chatId = update.getMessage().getChatId();
		if (isNotAllowedToSendContent(chatId, appUser)) {
			return;
		}

		try {
			AppDocument doc = fileService.processDoc(update.getMessage());
			String link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
			var answer = "Документ успешно загружен! Ссылка для скачивания:\n" + link;
			sendAnswer(answer, chatId);
		} catch (UploadFileException e) {
			log.error(e);
			String error = "К сожалению загрузка файла не удалась. Повторите попытку позже.";
			sendAnswer(error, chatId);
		}
	}

	@Override
	public void processPhotoMessage(Update update) {
		saveRawData(update);
		var appUser = findOrSaveAppUser(update);
		var chatId = update.getMessage().getChatId();
		if (isNotAllowedToSendContent(chatId, appUser)) {
			return;
		}

		try {
			AppPhoto photo = fileService.processPhoto(update.getMessage());
			String link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
			var answer = "Фото успешно загружено! Ссылка для скачивания:\n" + link;
			sendAnswer(answer, chatId);
		} catch (UploadFileException e) {
			log.error(e);
			String error = "К сожалению загрузка фото не удалась. Повторите попытку позже.";
			sendAnswer(error, chatId);
		}


	}

	private boolean isNotAllowedToSendContent(Long chatId, AppUser appUser) {
		var userState = appUser.getUserState();
		if (!appUser.getIsActive()) {
			var error = "Зарегистрируйтесь или активируйте свою учётную запись для загрузки контента";
			sendAnswer(error, chatId);
			return true;
		} else if (!BASIC_STATE.equals(userState)) {
			var error = "Отмените текущую команду с помощью /cancel для отправки файлов";
			sendAnswer(error, chatId);
			return true;
		}
		return false;
	}

	private void sendAnswer(String output, Long chatId) {
		SendMessage sendMessage = new SendMessage();
		sendMessage.setChatId(chatId);
		sendMessage.setText(output);
		producerService.produceAnswer(sendMessage);
	}

	private String processServiceCommand(AppUser appUser, String text) {
		var serviceCommand = ServiceCommand.fromValue(text);
		if (REGISTRATION.equals(serviceCommand)) {
			return appUserService.registerUser(appUser);
		} else if (HELP.equals(serviceCommand)) {
			return help();
		} else if (START.equals(serviceCommand)) {
			return "Приветствую! Чтобы посмотреть список доступных команд введите /help";
		} else {
			return "Неизвестная команда! Чтобы посмотреть список доступных команд введите /help";
		}
	}

	private String help() {
		return "Список доступных команд:\n" +
		       "/cancel - отмена выполнения текущей команды;\n" +
		       "/registration - регистрация пользователя.";
	}

	private String cancelProcess(AppUser appUser) {
		appUser.setUserState(BASIC_STATE);
		appUserDao.save(appUser);
		return "Команда отменена!";
	}

	private AppUser findOrSaveAppUser(Update update) {
		User telegramUser = update.getMessage().getFrom();
		// persistent - представлен в БД, имеет заполненный первичный ключ и связан с сессией hibernate,
		// которую под копотом у себя использует SpringData
		var optionalAppUser = appUserDao.findByTelegramUserId(telegramUser.getId());
		if (optionalAppUser.isEmpty()) {
			AppUser transientAppUser = AppUser.builder()
					.telegramUserId(telegramUser.getId())
					.username(telegramUser.getUserName())
					.firstName(telegramUser.getFirstName())
					.lastName(telegramUser.getLastName())
					.isActive(false)
					.userState(BASIC_STATE).build();
			return appUserDao.save(transientAppUser);
			// save() озвращает тот же объект, но с первичным ключом и привязкой к сессии hibernate
		}
		return optionalAppUser.get();
	}

	private void saveRawData(Update update) {
		RawData rawData = RawData.builder().event(update).build();
		rawDataDAO.save(rawData);
	}
}
