package com.example.sampleapi.user;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class DuplicateEmailException extends ApiException {

    public DuplicateEmailException(String email) {
        super(HttpStatus.CONFLICT, "A user with email " + email + " already exists");
    }
}
