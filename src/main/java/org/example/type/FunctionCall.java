package org.example.type;

import lombok.Builder;
import lombok.Singular;

import java.util.List;

@Builder
public record FunctionCall(
    Function function,
    @Singular
    List<Value> arguments
) {
}
