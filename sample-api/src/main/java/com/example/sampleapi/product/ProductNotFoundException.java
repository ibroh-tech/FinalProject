package com.example.sampleapi.product;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class ProductNotFoundException extends ApiException {

    public ProductNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Product not found: " + id);
    }
}
