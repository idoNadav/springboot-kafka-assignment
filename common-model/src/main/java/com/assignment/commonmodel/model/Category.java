package com.assignment.commonmodel.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Category {
    STANDARD("standard"),
    PERISHABLE("perishable"),
    DIGITAL("digital"),
    @JsonEnumDefaultValue
    UNKNOWN("unknown");
    private final String value;
    Category(String v) { this.value = v; }

    @JsonValue
    public String getValue() { return value; }

    @JsonCreator
    public static Category from(String v) {
        if (v == null) return UNKNOWN;
        for (var c : values()) {
            if (c.value.equalsIgnoreCase(v)) return c;
        }
        return UNKNOWN;
    }

}
