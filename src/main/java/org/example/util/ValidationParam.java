package org.example.util;

import java.util.function.BooleanSupplier;

public record ValidationParam(
        BooleanSupplier condition,
        int line,
        String message
) {
}
