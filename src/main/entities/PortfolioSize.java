package main.entities;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PortfolioSize {
    LOW("LOW"),
    BALANCED("BALANCED"),
    HIGH("HIGH");

    @JsonValue
    private final String value;

    PortfolioSize(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PortfolioSize fromValue(String value) {
        for (PortfolioSize ps : PortfolioSize.values()) {
            if (ps.value.equalsIgnoreCase(value)) {
                return ps;
            }
        }
        throw new IllegalArgumentException("Unknown portfolio size: " + value);
    }
}
