package com.example.sampleapi.auth;

import org.springframework.http.HttpStatus;

import com.example.sampleapi.common.ApiException;

public class DuplicateAccountException extends ApiException {

    public DuplicateAccountException(String email) {
        super(HttpStatus.CONFLICT, "An account with email " + email + " already exists");
    }
}
