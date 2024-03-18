package ru.dbhub.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NewsSourceConfigRepository extends JpaRepository<NewsSourceConfigModel, Long> {
}
