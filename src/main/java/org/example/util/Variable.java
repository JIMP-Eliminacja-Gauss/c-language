package org.example.util;

import org.example.type.Type;

public record Variable(
        String value,
        String id,
        Type type
) {
}
