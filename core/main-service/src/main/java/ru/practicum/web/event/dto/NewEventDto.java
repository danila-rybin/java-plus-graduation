package ru.practicum.web.event.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.web.event.entity.Location;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "Annotation must not be blank")
    @Size(min = 20, max = 2000, message = "Annotation length must be between 20 and 2000 characters")
    private String annotation;

    @NotNull(message = "Category must not be null")
    private Long category;

    @NotBlank(message = "Description must not be blank")
    @Size(min = 20, max = 7000, message = "Description length must be between 20 and 7000 characters")
    private String description;

    @NotBlank(message = "Event date must not be blank")
    private String eventDate;

    @NotNull(message = "Location must not be null")
    private Location location;

    private Boolean paid = false;

    @PositiveOrZero(message = "Participant limit must be positive or zero")
    private Integer participantLimit = 0;

    private Boolean requestModeration = true;

    @NotBlank(message = "Title must not be blank")
    @Size(min = 3, max = 120, message = "Title length must be between 3 and 120 characters")
    private String title;
}