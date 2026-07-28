package ru.practicum.web.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.web.admin.dto.CategoryDto;
import ru.practicum.web.admin.dto.UserShortDto;
import ru.practicum.web.event.entity.Location;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {
    private Long id;
    private String title;
    private String annotation;
    private String description;
    private String eventDate;
    private CategoryDto category;
    private UserShortDto initiator;
    private Location location;
    private Boolean paid;
    private Integer participantLimit;
    private Boolean requestModeration;
    private String state;
    private String createdOn;
    private String publishedOn;
    private Long views;
    private Long confirmedRequests;
}