package dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ForeignExchangePriceRequest(
    @NotBlank
    @Pattern(regexp = "[A-Z]{3}/[A-Z]{3}", message = "Currency pair must use the format USD/GBP")
    String currencyPair
) {}