package ru.practicum.web.admin.service;

import ru.practicum.web.admin.dto.CompilationDto;
import ru.practicum.web.admin.dto.NewCompilationDto;
import ru.practicum.web.admin.entity.UpdateCompilationRequest;

public interface AdminCompilationService {

    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long id, UpdateCompilationRequest dto);

    void delete(Long id);
}