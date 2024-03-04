package org.codesystem.server.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.codesystem.server.enums.packages.PackageStatusInternal;

@Converter
public class PackageStatusInternalConverter implements AttributeConverter<PackageStatusInternal, String> {
    @Override
    public String convertToDatabaseColumn(PackageStatusInternal packageStatusInternal) {
        if (packageStatusInternal == null) {
            return null;
        }
        return packageStatusInternal.name();
    }

    @Override
    public PackageStatusInternal convertToEntityAttribute(String s) {
        try {
            return Enum.valueOf(PackageStatusInternal.class, s);
        } catch (Exception e) {
            return PackageStatusInternal.ERROR;
        }
    }
}
