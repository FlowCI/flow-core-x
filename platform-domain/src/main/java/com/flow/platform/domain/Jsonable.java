package com.flow.platform.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.text.SimpleDateFormat;

/**
 * Created by gy@fir.im on 29/05/2017.
 * Copyright fir.im
 */
public abstract class Jsonable implements Serializable {

    public final static SimpleDateFormat DOMAIN_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss:SSS");

    public final static Gson GSON_CONFIG = new GsonBuilder()
            .setDateFormat(DOMAIN_DATE_FORMAT.toPattern())
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
