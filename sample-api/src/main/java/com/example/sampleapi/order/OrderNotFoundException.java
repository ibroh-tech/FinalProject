package com.example.sampleapi.order;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class OrderNotFoundException extends ApiException {

    public OrderNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Order not found: " + id);
    }
}
