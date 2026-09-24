package main.entities;

/**
 * Utility class for converting PortfolioSize enum to/from database values.
 * With MyBatis, this class provides conversion support without JPA annotations.
 */
public class PortfolioSizeConverter {

    public static String convertToDatabaseColumn(PortfolioSize attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    public static PortfolioSize convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PortfolioSize.fromValue(dbData);
    }
}
