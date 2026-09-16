package entities;

public enum PortfolioSize {
    LOW("Low"),
    BALANCED("Balanced"),
    HIGH("High");

    private final String value;

    PortfolioSize(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }


}
