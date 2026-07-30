package ru.practicum.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.admin.dto.CompilationDto;
import ru.practicum.web.admin.dto.NewCompilationDto;
import ru.practicum.web.admin.entity.Compilation;
import ru.practicum.web.admin.entity.UpdateCompilationRequest;
import ru.practicum.web.admin.mapper.CompilationMapper;
import ru.practicum.web.admin.repository.CompilationRepository;
import ru.practicum.web.admin.validation.CompilationValidator;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCompilationServiceImpl implements AdminCompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationValidator validator;

    @Override
    public CompilationDto create(NewCompilationDto dto) {
        log.info("Создание новой подборки: '{}'", dto.getTitle());

        validator.validateCreateRequest(dto);

        Compilation compilation = new Compilation();
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned() != null ? dto.getPinned() : false);
        compilation.setEvents(getEventsFromIds(dto.getEvents()));

        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка создана с id={}, заголовок='{}', закреплена={}, событий={}",
                saved.getId(), saved.getTitle(), saved.getPinned(),
                saved.getEvents() != null ? saved.getEvents().size() : 0);
        return CompilationMapper.toDto(saved);
    }

    @Override
    public CompilationDto update(Long id, UpdateCompilationRequest dto) {
        log.info("Обновление подборки с id={}", id);

        validator.validateUpdateRequest(dto);

        Compilation compilation = getCompilationOrThrow(id);
        String oldTitle = compilation.getTitle();
        Boolean oldPinned = compilation.getPinned();
        int oldEventsCount = compilation.getEvents() != null ? compilation.getEvents().size() : 0;

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
            log.debug("Обновлен заголовок подборки: '{}' -> '{}'", oldTitle, dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
            log.debug("Обновлен статус закрепления подборки: {} -> {}", oldPinned, dto.getPinned());
        }
        if (dto.getEvents() != null) {
            compilation.setEvents(getEventsFromIds(dto.getEvents()));
            log.debug("Обновлен список событий подборки: {} -> {} событий",
                    oldEventsCount, dto.getEvents().size());
        }

        Compilation updated = compilationRepository.save(compilation);
        log.info("Подборка с id={} успешно обновлена", id);
        return CompilationMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление подборки с id={}", id);

        validator.validateCompilationExists(compilationRepository.existsById(id), id);
        compilationRepository.deleteById(id);
        log.info("Подборка с id={} удалена", id);
    }

    private Compilation getCompilationOrThrow(Long id) {
        return compilationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Подборка с id={} не найдена", id);
                    return new NotFoundException("Compilation with id=" + id + " was not found");
                });
    }

    private List<Event> getEventsFromIds(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            log.debug("Список событий пуст");
            return new ArrayList<>();
        }

        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            List<Long> foundIds = events.stream().map(Event::getId).collect(Collectors.toList());
            List<Long> notFoundIds = eventIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            log.warn("События с id={} не найдены", notFoundIds);
            throw new NotFoundException("Events with ids=" + notFoundIds + " not found");
        }

        log.debug("Найдено {} событий из запрошенных {}", events.size(), eventIds.size());
        return events;
    }
}