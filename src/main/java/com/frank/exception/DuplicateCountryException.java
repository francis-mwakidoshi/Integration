package com.frank.exception;

public class DuplicateCountryException extends RuntimeException {

    public DuplicateCountryException(String isoCode) {
        super("Country already exists with ISO code: " + isoCode);
    }
}
