package com.flow.platform.cc.dao.adaptor.list;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by gy@fir.im on 28/06/2017.
 * Copyright fir.im
 */
public class ListThrowableAdaptor extends ListAdaptor {

    @Override
    protected Type getTargetType() {
        TypeToken<List<Throwable>> typeToken = new TypeToken<List<Throwable>>() {};
        return typeToken.getType();
    }
}
