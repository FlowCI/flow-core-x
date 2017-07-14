package com.flow.platform.cc.dao.adaptor;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by gy@fir.im on 28/06/2017.
 * Copyright fir.im
 */
public class MapAdaptor extends BaseAdaptor {

    @Override
    public Class returnedClass() {
        return Map.class;
    }

    @Override
    protected Type getTargetType() {
        TypeToken<Map<String, String>> typeToken = new TypeToken<Map<String, String>>() {};
        return typeToken.getType();
    }
}
