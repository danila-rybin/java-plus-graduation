package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.CategoryDto;

@FeignClient(
        name = "category-service",
        contextId = "categoryClient",
        path = "/admin/categories"
)
public interface CategoryClient {

    @GetMapping("/{catId}")
    ResponseEntity<CategoryDto> getById(@PathVariable("catId") Long catId);
}
