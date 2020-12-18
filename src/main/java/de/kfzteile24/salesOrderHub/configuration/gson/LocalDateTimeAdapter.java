package de.kfzteile24.salesOrderHub.configuration.gson;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;

public final class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    @Override
    public void write(final JsonWriter jsonWriter, final LocalDateTime localDate ) throws IOException {
        jsonWriter.value(localDate.toString());
    }

    @Override
    public LocalDateTime read( final JsonReader jsonReader ) throws IOException {
        return LocalDateTime.parse(jsonReader.nextString());
    }
}
