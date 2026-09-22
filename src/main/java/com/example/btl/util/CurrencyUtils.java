package com.example.btl.util;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;

@Component
public class CurrencyUtils {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,###.##");

    public String format(BigDecimal amount) {
        if (amount == null) {
            return FORMAT.format(BigDecimal.ZERO);
        }
        return FORMAT.format(amount);
    }
}