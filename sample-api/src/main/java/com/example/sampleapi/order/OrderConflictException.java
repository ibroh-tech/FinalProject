package com.example.sampleapi.order;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class OrderConflictException extends ApiException {

    public OrderConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
