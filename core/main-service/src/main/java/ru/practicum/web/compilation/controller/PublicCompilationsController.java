package ru.practicum.web.compilation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.admin.dto.CompilationDto;
import ru.practicum.web.admin.mapper.CompilationMapper;
import ru.practicum.web.admin.repository.CompilationRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
public class PublicCompilationsController {

    private final CompilationRepository compilationRepository;

    @GetMapping
    public ResponseEntity<List<CompilationDto>> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<CompilationDto> compilations;
        if (pinned != null) {
            compilations = compilationRepository.findAllByPinned(pinned, pageable)
                    .stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            compilations = compilationRepository.findAll(pageable)
                    .stream()
                    .map(CompilationMapper::toDto)
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(compilations);
    }

    @GetMapping("/{compId}")
    public ResponseEntity<CompilationDto> getCompilation(@PathVariable Long compId) {
        return compilationRepository.findById(compId)
                .map(CompilationMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}