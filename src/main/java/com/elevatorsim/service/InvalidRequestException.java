package com.elevatorsim.service;

/** Bad input. The CLI exits with code 2; the API answers 400. */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
