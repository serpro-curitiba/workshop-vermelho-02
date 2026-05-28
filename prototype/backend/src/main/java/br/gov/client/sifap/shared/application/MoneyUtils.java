package br.gov.client.sifap.shared.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

    private MoneyUtils() {
    }

    public static BigDecimal truncate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.DOWN);
        }
        return value.setScale(2, RoundingMode.DOWN);
    }
}