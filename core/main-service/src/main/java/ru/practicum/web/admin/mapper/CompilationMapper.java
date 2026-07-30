package ru.practicum.web.admin.mapper;

import ru.practicum.web.admin.dto.CompilationDto;
import ru.practicum.web.admin.entity.Compilation;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.mapper.EventMapper;

import java.util.List;
import java.util.stream.Collectors;

public class CompilationMapper {

    public static CompilationDto toDto(Compilation compilation) {
        if (compilation == null) {
            return null;
        }

        List<EventShortDto> eventDtos = compilation.getEvents() != null ?
                compilation.getEvents().stream()
                        .map(EventMapper::toShortDto)
                        .collect(Collectors.toList()) :
                List.of();

        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.getPinned())
                .events(eventDtos)
                .build();
    }

    public static Compilation toEntity(CompilationDto dto) {
        if (dto == null) {
            return null;
        }

        Compilation compilation = new Compilation();
        compilation.setId(dto.getId());
        compilation.setTitle(dto.getTitle());
        compilation.setPinned(dto.getPinned());
        return compilation;
    }
}