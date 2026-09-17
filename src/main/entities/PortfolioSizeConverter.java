package main.entities;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class PortfolioSizeConverter implements AttributeConverter<PortfolioSize, String> {

    @Override
    public String convertToDatabaseColumn(PortfolioSize attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public PortfolioSize convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PortfolioSize.fromValue(dbData);
    }
}
