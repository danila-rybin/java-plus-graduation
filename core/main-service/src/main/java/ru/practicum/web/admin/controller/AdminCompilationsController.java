package ru.practicum.web.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.admin.dto.CompilationDto;
import ru.practicum.web.admin.dto.NewCompilationDto;
import ru.practicum.web.admin.entity.UpdateCompilationRequest;
import ru.practicum.web.admin.service.AdminCompilationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Validated
public class AdminCompilationsController {

    private final AdminCompilationService service;

    @PostMapping
    public ResponseEntity<CompilationDto> create(@RequestBody @Valid NewCompilationDto dto) {
        CompilationDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{compId}")
    public ResponseEntity<CompilationDto> update(
            @PathVariable Long compId,
            @RequestBody @Valid UpdateCompilationRequest dto
    ) {
        CompilationDto updated = service.update(compId, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{compId}")
    public ResponseEntity<Void> delete(@PathVariable Long compId) {
        service.delete(compId);
        return ResponseEntity.noContent().build();
    }
}