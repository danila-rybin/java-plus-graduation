package ru.practicum.web.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class NewCompilationDto {
    private List<Long> events;

    private Boolean pinned = false;

    @NotBlank(message = "Title must not be blank")
    @Size(min = 1, max = 50, message = "Title length must be between 1 and 50 characters")
    private String title;
}