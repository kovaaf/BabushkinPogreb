package ru.relex.service.impl;

import lombok.extern.log4j.Log4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import ru.relex.dao.AppDocumentDao;
import ru.relex.dao.AppPhotoDao;
import ru.relex.entity.AppDocument;
import ru.relex.entity.AppPhoto;
import ru.relex.entity.BinaryContent;
import ru.relex.service.FileService;
import ru.relex.utils.CryptoTool;

import java.io.File;
import java.io.IOException;

@Log4j
@Service
public class FileServiceImpl implements FileService {
	private final AppDocumentDao appDocumentDao;
	private final AppPhotoDao appPhotoDao;
	private final CryptoTool cryptoTool;

	public FileServiceImpl(AppDocumentDao appDocumentDao, AppPhotoDao appPhotoDao, CryptoTool cryptoTool) {
		this.appDocumentDao = appDocumentDao;
		this.appPhotoDao = appPhotoDao;
		this.cryptoTool = cryptoTool;
	}

	@Override
	public AppDocument getDocument(String hash) {
		var id = cryptoTool.idOf(hash);
		if (id == null) {
			return null;
		}
		return appDocumentDao.findById(id).orElse(null);
	}

	@Override
	public AppPhoto getPhoto(String hash) {
		var id = cryptoTool.idOf(hash);
		if (id == null) {
			return null;
		}
		return appPhotoDao.findById(id).orElse(null);
	}

	@Override
	public FileSystemResource getFileSystemResource(BinaryContent binaryContent) {
		try {
			// TODO добавить генерацию имени временного файла (они могут быть неуникальными)
			File temp = File.createTempFile("tempFile", ".bin");
			temp.deleteOnExit();
			FileUtils.writeByteArrayToFile(temp, binaryContent.getFileAsArrayOfBytes());
			return new FileSystemResource(temp);
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}
}