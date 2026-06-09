package com.psa.capstone.be.utilities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateTimeDeserializer extends
        JsonDeserializer<LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        String dateTimeString = p.getText();
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateTimeString, FORMATTER);
        return zonedDateTime.withZoneSameInstant(ZONE_ID).toLocalDateTime();
    }
}