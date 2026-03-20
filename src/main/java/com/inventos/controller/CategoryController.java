package com.inventos.controller;

import com.inventos.dto.response.ApiResponse;
import com.inventos.entity.Category;
import com.inventos.exception.*;
import com.inventos.repository.CategoryRepository;
import com.inventos.repository.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/categories") @RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository  productRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAll() {
        List<Map<String, Object>> result = categoryRepository.findAll().stream()
            .map(c -> Map.<String, Object>of(
                "id",           c.getId(),
                "name",         c.getName(),
                "description",  c.getDescription() != null ? c.getDescription() : "",
                "createdAt",    c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
                "productCount", productRepository.countActiveByCategoryId(c.getId())
            ))
            .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> create(
            @Valid @RequestBody CategoryRequest req) {
        if (categoryRepository.existsByNameIgnoreCase(req.getName()))
            throw new ConflictException("Category '" + req.getName() + "' already exists");
        Category saved = categoryRepository.save(
            Category.builder().name(req.getName().trim()).description(req.getDescription()).build());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Category created", toMap(saved, 0L)));
    }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> update(
            @PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (!cat.getName().equalsIgnoreCase(req.getName())
                && categoryRepository.existsByNameIgnoreCase(req.getName()))
            throw new ConflictException("Category '" + req.getName() + "' already exists");
        cat.setName(req.getName().trim());
        if (req.getDescription() != null) cat.setDescription(req.getDescription());
        Category saved = categoryRepository.save(cat);
        long count = productRepository.countActiveByCategoryId(saved.getId());
        return ResponseEntity.ok(ApiResponse.ok("Category updated", toMap(saved, count)));
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Category cat = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        long count = productRepository.countActiveByCategoryId(id);
        if (count > 0)
            throw new BusinessException(
                "Cannot delete '" + cat.getName() + "' — it has " + count +
                " active product(s). Reassign or delete those products first.");
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.ok("Category deleted", null));
    }

    private Map<String, Object> toMap(Category c, long productCount) {
        return Map.of(
            "id",           c.getId(),
            "name",         c.getName(),
            "description",  c.getDescription() != null ? c.getDescription() : "",
            "createdAt",    c.getCreatedAt() != null ? c.getCreatedAt().toString() : "",
            "productCount", productCount
        );
    }

    @Data static class CategoryRequest {
        @NotBlank @Size(max = 80)  private String name;
        @Size(max = 255)           private String description;
    }
}
