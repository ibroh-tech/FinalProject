package com.example.sampleapi.auth;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class InvalidCredentialsException extends ApiException {

    public InvalidCredentialsException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
