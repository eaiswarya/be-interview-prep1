package com.interviewprep.exception;

/** A request that passed annotation validation but breaks a rule spanning fields or the domain. Maps to 400. */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
