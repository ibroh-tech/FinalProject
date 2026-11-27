package com.example.sampleapi.user;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class UserNotFoundException extends ApiException {

    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "User not found: " + id);
    }
}
