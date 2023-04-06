package ru.relex.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.relex.entity.AppUser;

@Repository
public interface AppUserDao extends JpaRepository<AppUser, Long> {
	AppUser findAppUserByTelegramUserId(Long id);
}
