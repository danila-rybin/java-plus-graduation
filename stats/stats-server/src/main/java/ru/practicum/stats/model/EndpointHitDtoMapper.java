package ru.practicum.stats.model;

import ru.practicum.dto.EndpointHitDto;

public class EndpointHitDtoMapper {

    public static EndpointHitDto toDto(EndpointHit endpointHit) {
        if (endpointHit == null) {
            return null;
        }

        EndpointHitDto dto = new EndpointHitDto();
        dto.setId(endpointHit.getId());
        dto.setApp(endpointHit.getApp());
        dto.setUri(endpointHit.getUri());
        dto.setIp(endpointHit.getIp());
        dto.setTimestamp(endpointHit.getTimestamp());
        return dto;
    }

    public static EndpointHit toEntity(EndpointHitDto dto) {
        if (dto == null) {
            return null;
        }

        EndpointHit hit = new EndpointHit();
        hit.setId(dto.getId());
        hit.setApp(dto.getApp());
        hit.setUri(dto.getUri());
        hit.setIp(dto.getIp());
        hit.setTimestamp(dto.getTimestamp());
        return hit;
    }
}