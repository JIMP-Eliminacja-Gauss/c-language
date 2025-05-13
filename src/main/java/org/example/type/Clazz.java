package org.example.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class Clazz implements AbstractType {
    private final String name;
    private final Map<String, Field> fields;
    private final Map<String, Function> methods;

    public Clazz(String name) {
        this(name, new HashMap<>(), new HashMap<>());
    }

    public static Clazz of(String name) {
        return new Clazz(name, new HashMap<>(), new HashMap<>());
    }

    public static String methodNameFor(Clazz clazz, String methodName) {
        Objects.requireNonNull(clazz);
        return clazz.getName() + "_" + methodName;
    }

    public void addField(String fieldName, Value value) {
        final var offset = fields.size();
        fields.put(fieldName, new Field(offset, value));
    }

    public void addMethod(String methodName, Function method) {
        methods.put(methodName, method);
    }

    public Field getField(String fieldName) {
        if (!fields.containsKey(fieldName)) {
            throw new IllegalArgumentException("Field " + fieldName + " does not exist in class " + name);
        }
        return fields.get(fieldName);
    }

    public Function getMethod(String methodName) {
        if (!methods.containsKey(methodName)) {
            throw new IllegalArgumentException("Method " + methodName + " does not exist in class " + name);
        }
        return methods.get(methodName);
    }

    @Override
    public String getLlvmRepresentation() {
        return "%" + name;
    }

    @Override
    public String getLlvmStringRepresentation() {
        return "";
    }

    @Override
    public String getLlvmComparator() {
        return "";
    }

    @Override
    public String getDefaultValue() {
        return "null";
    }
}
