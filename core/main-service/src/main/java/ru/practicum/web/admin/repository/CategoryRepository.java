package ru.practicum.web.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.web.admin.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}