package com.horizon.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Encrypts {@code users.national_id} on write and decrypts it on read. */
@Converter
public class NationalIdConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return null;
        }
        return FieldEncryptor.instance().encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return FieldEncryptor.instance().decrypt(dbData);
    }
}
