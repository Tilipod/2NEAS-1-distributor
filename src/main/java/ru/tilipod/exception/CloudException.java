package ru.tilipod.exception;

import lombok.Getter;

@Getter
public class CloudException extends RuntimeException {

    public CloudException(String message) {
        super(message);
    }
}
