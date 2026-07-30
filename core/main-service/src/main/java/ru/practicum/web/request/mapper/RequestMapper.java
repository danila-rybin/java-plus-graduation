package ru.practicum.web.request.mapper;

import ru.practicum.web.request.dto.RequestDto;
import ru.practicum.web.request.entity.Request;

public class RequestMapper {

    public static RequestDto toDto(Request request) {
        if (request == null) {
            return null;
        }

        return RequestDto.builder()
                .id(request.getId())
                .created(request.getCreated().withNano(0))
                .event(request.getEvent().getId())
                .requester(request.getRequester().getId())
                .status(request.getStatus() != null ? request.getStatus().name() : null)
                .build();
    }
}