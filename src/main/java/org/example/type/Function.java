package org.example.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Builder
@Getter
public class Function {
    private static final Logger logger = Logger.getLogger(Function.class.getName());
    private String name;

    @Builder.Default
    private List<Parameter> parameters = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    @Builder.Default
    private Set<String> parameterNames = new HashSet<>();

    private Type returnType;

    public void addParameter(Parameter parameter) {
        if (parameterNames.contains(parameter.getName())) {
            logger.warning("Parameter " + parameter.getName() + " already exists");
            throw new IllegalArgumentException("Parameter name already exists: " + parameter.getName());
        }
        this.parameterNames.add(parameter.getName());
        this.parameters.add(parameter);
    }

}
