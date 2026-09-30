package com.fashion.backend.common.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GenderTarget {
    MEN,
    WOMEN,
    UNISEX;

    @JsonCreator
    public static GenderTarget fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return UNISEX;
        }
        String normalized = value.trim().toUpperCase();
        if ("FEMALE".equals(normalized) || "WOMEN".equals(normalized) || "WOMAN".equals(normalized)) {
            return WOMEN;
        }
        if ("MALE".equals(normalized) || "MEN".equals(normalized) || "MAN".equals(normalized)) {
            return MEN;
        }
        try {
            return GenderTarget.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return UNISEX;
        }
    }
}
