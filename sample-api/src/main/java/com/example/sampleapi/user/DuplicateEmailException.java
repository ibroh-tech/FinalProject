package com.example.sampleapi.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("A user with email " + email + " already exists");
    }
}
