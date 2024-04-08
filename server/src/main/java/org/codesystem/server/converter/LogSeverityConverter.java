package org.codesystem.server.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.codesystem.server.enums.agent.OperatingSystem;
import org.codesystem.server.enums.log.Severity;

@Converter
public class LogSeverityConverter implements AttributeConverter<Severity, String> {
    @Override
    public String convertToDatabaseColumn(Severity severity) {
        if (severity == null) {
            return null;
        }
        return severity.name();
    }

    @Override
    public Severity convertToEntityAttribute(String s) {
        try {
            return Enum.valueOf(Severity.class, s);
        } catch (Exception e) {
            return Severity.ERROR;
        }
    }
}
