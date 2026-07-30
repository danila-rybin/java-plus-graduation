package ru.practicum.web.admin.service;

import ru.practicum.dto.CompilationDto;
import ru.practicum.dto.NewCompilationDto;
import ru.practicum.dto.UpdateCompilationRequest;

public interface AdminCompilationService {

    CompilationDto create(NewCompilationDto dto);

    CompilationDto update(Long id, UpdateCompilationRequest dto);

    void delete(Long id);
}
