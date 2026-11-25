package com.ecommerce.batch.imports;

/** Thrown by the processor for a line that must not be imported. The message is the reason shown to the admin. */
public class InvalidRowException extends RuntimeException {

    public InvalidRowException(String reason) {
        super(reason);
    }
}
