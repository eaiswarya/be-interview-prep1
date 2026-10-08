package com.interviewprep.exception;

/** A ConflictException, so it maps to 409. */
public class InsufficientStockException extends ConflictException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Insufficient stock for product " + productId + ": requested " + requested
                + ", available " + available);
    }
}
