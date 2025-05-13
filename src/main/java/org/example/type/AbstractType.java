package org.example.type;

public interface AbstractType {
    String getLlvmRepresentation();

    String getLlvmStringRepresentation();

    String getLlvmComparator();

    String getDefaultValue();
}
