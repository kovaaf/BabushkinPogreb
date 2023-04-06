package ru.relex.service.impl;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import ru.relex.dao.AppDocumentDao;
import ru.relex.dao.AppPhotoDao;
import ru.relex.dao.BinaryContentDao;
import ru.relex.entity.AppDocument;
import ru.relex.entity.AppPhoto;
import ru.relex.entity.BinaryContent;
import ru.relex.exceptions.UploadFileException;
import ru.relex.service.FileService;
import ru.relex.service.enums.LinkType;
import ru.relex.utils.CryptoTool;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

@Log4j
@Service
public class FileServiceImpl implements FileService {
	@Value("${token}")
	private String token;
	@Value("${service.file_info.uri}")
	private String fileInfoUri;
	@Value("${service.file_storage.uri}")
	private String fileStorageUri;
	@Value("${link.address}")
	private String linkAddress;
	private final AppDocumentDao appDocumentDao;
	private final AppPhotoDao appPhotoDao;
	private final BinaryContentDao binaryContentDao;
	private final CryptoTool cryptoTool;


	public FileServiceImpl(AppDocumentDao appDocumentDao, AppPhotoDao appPhotoDao, BinaryContentDao binaryContentDao,
	                       CryptoTool cryptoTool) {
		this.appDocumentDao = appDocumentDao;
		this.appPhotoDao = appPhotoDao;
		this.binaryContentDao = binaryContentDao;
		this.cryptoTool = cryptoTool;
	}

	@SneakyThrows
	@Override
	public AppDocument processDoc(Message telegramMessage) {
		Document telegramDoc = telegramMessage.getDocument();
		String fileId = telegramDoc.getFileId();
		ResponseEntity<String> response = getFilePath(fileId);
		if (response.getStatusCode() == HttpStatus.OK) {
			BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
			AppDocument transientAppDoc = buildTransientAppDoc(telegramDoc, persistentBinaryContent);
			return appDocumentDao.save(transientAppDoc);
		} else {
			throw new UploadFileException("Bad response from telegram service:" + response);
		}
	}


	@SneakyThrows
	@Override
	public AppPhoto processPhoto(Message telegramMessage) {
		var photoSizeCount = telegramMessage.getPhoto().size();
		var photoIndex = photoSizeCount > 1 ? telegramMessage.getPhoto().size() - 1 : 0;
		PhotoSize telegramPhoto = telegramMessage.getPhoto().get(photoIndex);
		String fileId = telegramPhoto.getFileId();
		ResponseEntity<String> response = getFilePath(fileId);
		if (response.getStatusCode() == HttpStatus.OK) {
			BinaryContent persistentBinaryContent = getPersistentBinaryContent(response);
			AppPhoto transientAppPhoto = buildTransientAppPhoto(telegramPhoto, persistentBinaryContent);
			return appPhotoDao.save(transientAppPhoto);
		} else {
			throw new UploadFileException("Bad response from telegram service:" + response);
		}
	}

	private BinaryContent getPersistentBinaryContent(ResponseEntity<String> response) throws JSONException {
		String filePath = getFilePath(response);
		byte[] fileInByte = downloadFile(filePath);
		BinaryContent transientBinaryContent = BinaryContent.builder()
		                                                    .fileAsArrayOfBytes(fileInByte)
		                                                    .build();
		return binaryContentDao.save(transientBinaryContent);
	}

	private String getFilePath(ResponseEntity<String> response) throws JSONException {
		JSONObject jsonObject = new JSONObject(response.getBody());
		String filePath = String.valueOf(jsonObject.getJSONObject("result")
		                                           .getString("file_path"));
		return filePath;
	}

	private AppPhoto buildTransientAppPhoto(PhotoSize telegramPhoto, BinaryContent persistentBinaryContent) {
		return AppPhoto.builder()
		                  .telegramField(telegramPhoto.getFileId())
		                  .binaryContent(persistentBinaryContent)
		                  .fileSize(telegramPhoto.getFileSize())
		                  .build();
	}

	private AppDocument buildTransientAppDoc(Document telegramDoc, BinaryContent persistentBinaryContent) {
		return AppDocument.builder()
		                  .telegramField(telegramDoc.getFileId())
		                  .docName(telegramDoc.getFileName())
		                  .binaryContent(persistentBinaryContent)
		                  .mimeType(telegramDoc.getMimeType())
		                  .fileSize(telegramDoc.getFileSize())
		                  .build();
	}

	private byte[] downloadFile(String filePath) {
		String fullUri = fileStorageUri.replace("{token}", token)
		                               .replace("{filePath}", filePath);
		URL urlObj;
		try {
			urlObj = new URL(fullUri);
		} catch (MalformedURLException e) {
			throw new UploadFileException(e);
		}

		// TODO подумать на оптимизация т.к. идёт скачивание одним большим файлом без разделения на чанки
		try (InputStream is = urlObj.openStream()) {
			return is.readAllBytes();
		} catch (IOException e) {
			throw new UploadFileException(urlObj.toExternalForm(), e);
		}
	}

	private ResponseEntity<String> getFilePath(String fileId) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<String> request = new HttpEntity<>(headers);

		return restTemplate.exchange(fileInfoUri, HttpMethod.GET, request, String.class, token, fileId);
	}

	@Override
	public String generateLink(Long docId, LinkType linkType) {
		var hash = cryptoTool.hashOf(docId);
		return "http://" + linkAddress + linkType +"?id=" + hash;
	}
}
