package com.example.sampleapi.order;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super(HttpStatus.CONFLICT,
                "Requested quantity " + requested + " exceeds available stock " + available
                        + " for product " + productId);
    }
}
