package ru.relex.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.relex.entity.AppPhoto;

public interface AppPhotoDao extends JpaRepository<AppPhoto, Long> {

}
