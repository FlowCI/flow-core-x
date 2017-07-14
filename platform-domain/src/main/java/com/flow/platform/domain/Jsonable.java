package com.flow.platform.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public abstract class Jsonable implements Serializable {

    public final static DateTimeFormatter DOMAIN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss:SSSZ");

    public final static Gson GSON_CONFIG = new GsonBuilder()
            .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
                @Override
                public void write(JsonWriter writer, ZonedDateTime value) throws IOException {
                    if (value != null) {
                        writer.value(value.format(DOMAIN_DATE_FORMAT));
                    } else{
                        writer.nullValue();
                    }
                }

                @Override
                public ZonedDateTime read(JsonReader reader) throws IOException {
                    String raw = reader.nextString();
                    try {
                        return ZonedDateTime.parse(raw, DOMAIN_DATE_FORMAT);
                    } catch (DateTimeParseException ignored) {
                        return null;
                    }
                }
            })
            .create();

    public static <T extends Jsonable> T parse(String json, Class<T> tClass) {
        return GSON_CONFIG.fromJson(json, tClass);
    }

    public static <T extends Jsonable> T parse(byte[] bytes, Class<T> tClass) {
        return GSON_CONFIG.fromJson(new String(bytes), tClass);
    }

    public String toJson() {
        return GSON_CONFIG.toJson(this);
    }

    public byte[] toBytes() {
        return toJson().getBytes();
    }
}
