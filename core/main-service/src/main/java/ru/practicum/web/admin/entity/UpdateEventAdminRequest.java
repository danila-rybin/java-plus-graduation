package ru.practicum.web.admin.entity;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UpdateEventAdminRequest {
    @Size(min = 20, max = 2000, message = "Annotation length must be between 20 and 2000 characters")
    private String annotation;

    private Long category;

    @Size(min = 20, max = 7000, message = "Description length must be between 20 and 7000 characters")
    private String description;

    private String eventDate;

    private Boolean paid;

    private Integer participantLimit;

    private Boolean requestModeration;

    private String stateAction;

    @Size(min = 3, max = 120, message = "Title length must be between 3 and 120 characters")
    private String title;
}