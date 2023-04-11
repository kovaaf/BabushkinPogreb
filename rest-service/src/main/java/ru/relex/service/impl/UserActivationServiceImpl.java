package ru.relex.service.impl;

import org.springframework.stereotype.Service;
import ru.relex.dao.AppUserDao;
import ru.relex.service.UserActivationService;
import ru.relex.utils.CryptoTool;

@Service
public class UserActivationServiceImpl implements UserActivationService {
	private final AppUserDao appUserDao;
	private final CryptoTool cryptoTool;

	public UserActivationServiceImpl(AppUserDao appUserDao, CryptoTool cryptoTool) {
		this.appUserDao = appUserDao;
		this.cryptoTool = cryptoTool;
	}

	@Override
	public boolean activation(String cryptoUserId) {
		var userId = cryptoTool.idOf(cryptoUserId);
		var optionalUser = appUserDao.findById(userId);
		if (optionalUser.isPresent()) {
			var user = optionalUser.get();
			user.setIsActive(true);
			appUserDao.save(user);
			return true;
		}
		return false;
	}
}
