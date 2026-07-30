package ru.practicum.web.admin.entity;

import lombok.Data;
import jakarta.validation.constraints.Size;

import java.util.List;

@Data
public class UpdateCompilationRequest {
    private List<Long> events;

    private Boolean pinned;

    @Size(min = 1, max = 50, message = "Title length must be between 1 and 50 characters")
    private String title;
}