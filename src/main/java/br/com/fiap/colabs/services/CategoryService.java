package br.com.fiap.colabs.services;

import br.com.fiap.colabs.config.EvictCatalogCaches;
import br.com.fiap.colabs.dtos.CategoryRequest;
import br.com.fiap.colabs.dtos.CategoryResponse;
import br.com.fiap.colabs.exceptions.BusinessConflictException;
import br.com.fiap.colabs.exceptions.ResourceNotFoundException;
import br.com.fiap.colabs.models.Category;
import br.com.fiap.colabs.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Cacheable(value = "categories", key = "#pageable.pageNumber + '_' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<CategoryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(CategoryResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @EvictCatalogCaches
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessConflictException("Já existe uma categoria com este nome");
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @EvictCatalogCaches
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));

        if (!category.getName().equalsIgnoreCase(request.name()) && categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new BusinessConflictException("Já existe outra categoria com este nome");
        }

        category.setName(request.name());
        category.setDescription(request.description());
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @EvictCatalogCaches
    @Transactional
    public void delete(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada: " + id));
        categoryRepository.delete(category);
    }
}