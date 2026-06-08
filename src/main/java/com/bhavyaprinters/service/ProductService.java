package com.bhavyaprinters.service;

import com.bhavyaprinters.dto.ProductDto;
import com.bhavyaprinters.dto.ProductInputDto;
import com.bhavyaprinters.dto.ProductUpdateDto;
import com.bhavyaprinters.entity.Product;
import com.bhavyaprinters.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductDto> listProducts() {
        return productRepository.findAllByOrderByCreatedAtAsc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public Optional<ProductDto> getProduct(Long id) {
        return productRepository.findById(id).map(this::toDto);
    }

    @Transactional
    public ProductDto createProduct(ProductInputDto input) {
        Product p = new Product();
        p.setName(input.getName());
        p.setDescription(input.getDescription());
        p.setPrice(BigDecimal.valueOf(input.getPrice()));
        p.setCategory(input.getCategory());
        p.setImageUrl(input.getImageUrl());
        return toDto(productRepository.save(p));
    }

    @Transactional
    public Optional<ProductDto> updateProduct(Long id, ProductUpdateDto update) {
        return productRepository.findById(id).map(p -> {
            if (update.getName() != null) p.setName(update.getName());
            if (update.getDescription() != null) p.setDescription(update.getDescription());
            if (update.getPrice() != null) p.setPrice(BigDecimal.valueOf(update.getPrice()));
            if (update.getCategory() != null) p.setCategory(update.getCategory());
            if (update.getImageUrl() != null) p.setImageUrl(update.getImageUrl());
            return toDto(productRepository.save(p));
        });
    }

    @Transactional
    public boolean deleteProduct(Long id) {
        if (!productRepository.existsById(id)) return false;
        productRepository.deleteById(id);
        return true;
    }

    public ProductDto toDto(Product p) {
        return new ProductDto(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice().doubleValue(),
                p.getCategory(),
                p.getImageUrl(),
                p.getCreatedAt().toString()
        );
    }
}
