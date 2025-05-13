package org.example.type;

import java.util.List;
import lombok.Builder;
import lombok.Singular;

@Builder
public record FunctionCall(Function function, @Singular List<Value> arguments) {}
