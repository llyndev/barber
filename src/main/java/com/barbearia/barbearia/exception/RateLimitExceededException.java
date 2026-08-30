package com.barbearia.barbearia.exception;

import lombok.Getter;

@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many attempts. Try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
