package ru.practicum.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.admin.dto.CategoryDto;
import ru.practicum.web.admin.dto.NewCategoryDto;
import ru.practicum.web.admin.entity.Category;
import ru.practicum.web.admin.mapper.CategoryMapper;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.admin.validation.CategoryValidator;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;
    private final CategoryValidator validator;

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        log.info("Создание новой категории: {}", dto.getName());

        validator.validateCategoryName(dto.getName());
        validator.checkCategoryNameUnique(dto.getName());

        try {
            Category category = Category.builder()
                    .name(dto.getName())
                    .build();
            Category saved = categoryRepository.save(category);
            log.info("Категория создана с id={}", saved.getId());
            return CategoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Ошибка уникальности при создании категории: {}", dto.getName());
            validator.checkCategoryNameUnique(dto.getName());
            throw e;
        }
    }

    @Override
    public CategoryDto update(Long id, CategoryDto dto) {
        log.info("Обновление категории с id={}: новое название '{}'", id, dto.getName());

        validator.validateCategoryName(dto.getName());
        validator.validateCategoryExists(id);
        validator.checkCategoryNameUniqueForUpdate(dto.getName(), id);

        Category category = getCategoryOrThrow(id);
        String oldName = category.getName();
        category.setName(dto.getName());

        Category updated = categoryRepository.save(category);
        log.info("Категория с id={} обновлена: '{}' -> '{}'", id, oldName, dto.getName());
        return CategoryMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление категории с id={}", id);

        validator.validateCategoryExists(id);
        checkCategoryNotInUse(id);

        categoryRepository.deleteById(id);
        log.info("Категория с id={} удалена", id);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        log.debug("Запрос списка категорий: from={}, size={}", from, size);

        validator.validatePagination(from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<CategoryDto> categories = categoryRepository.findAll(pageable)
                .stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено {} категорий", categories.size());
        return categories;
    }

    @Override
    public CategoryDto getById(Long id) {
        log.debug("Запрос категории с id={}", id);
        return CategoryMapper.toDto(getCategoryOrThrow(id));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Категория с id={} не найдена", id);
                    return new NotFoundException("Category with id=" + id + " was not found");
                });
    }

    private void checkCategoryNotInUse(Long categoryId) {
        if (eventRepository.existsByCategoryId(categoryId)) {
            log.warn("Попытка удалить категорию с id={}, которая используется в событиях", categoryId);
            throw new ConflictException("The category is not empty");
        }
    }
}