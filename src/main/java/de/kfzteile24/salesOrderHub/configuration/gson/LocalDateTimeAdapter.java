package de.kfzteile24.salesOrderHub.configuration.gson;


import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static java.time.format.DateTimeFormatter.*;

public final class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    private final DateTimeFormatter offsetDateTimeFormatter = ISO_OFFSET_DATE_TIME;
    private final DateTimeFormatter localDateTimeFormatter = ISO_LOCAL_DATE_TIME;
    private final DateTimeFormatter timeZoneDateTimeFormatter = ISO_ZONED_DATE_TIME;


    private final ZoneOffset offset = ZoneOffset.UTC;

    @Override
    public void write(final JsonWriter jsonWriter, final LocalDateTime localDate) throws IOException {
        jsonWriter.value(OffsetDateTime.of(localDate, offset).format(offsetDateTimeFormatter));
    }

    @Override
    public LocalDateTime read(final JsonReader jsonReader) throws IOException {
        final String dateStr = jsonReader.nextString();
        if (isValid(dateStr, localDateTimeFormatter)) {
            return LocalDateTime.parse(dateStr, localDateTimeFormatter);
        } else if (isValid(dateStr, timeZoneDateTimeFormatter)) {
            return ZonedDateTime.parse(dateStr, timeZoneDateTimeFormatter).toLocalDateTime();
        } else {
            return OffsetDateTime.parse(dateStr, offsetDateTimeFormatter).toLocalDateTime();
        }
    }

    protected final Boolean isValid(String dateString, DateTimeFormatter dateTimeFormatter) {
        try {
            dateTimeFormatter.parse(dateString);
        } catch (DateTimeParseException e) {
            return false;
        }

        return true;
    }
}

