package com.example.sampleapi.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(
                request.name().strip(),
                normalizeDescription(request.description()),
                request.price(),
                request.stock());
        return ProductResponse.from(productRepository.save(product));
    }

    public ProductResponse get(Long id) {
        return ProductResponse.from(findProduct(id));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findProduct(id);
        product.update(
                request.name().strip(),
                normalizeDescription(request.description()),
                request.price(),
                request.stock());
        return ProductResponse.from(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(findProduct(id));
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private static String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String trimmed = description.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
