package org.codesystem.server.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.codesystem.server.enums.agent.OperatingSystem;

@Converter
public class OperatingSystemConverter implements AttributeConverter<OperatingSystem, String> {
    @Override
    public String convertToDatabaseColumn(OperatingSystem packageStatusInternal) {
        if (packageStatusInternal == null) {
            return null;
        }
        return packageStatusInternal.name();
    }

    @Override
    public OperatingSystem convertToEntityAttribute(String s) {
        try {
            return Enum.valueOf(OperatingSystem.class, s);
        } catch (Exception e) {
            return OperatingSystem.UNKNOWN;
        }
    }
}
