package com.flow.platform.cc.dao.adaptor.list;

import com.flow.platform.cc.dao.adaptor.BaseAdaptor;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by gy@fir.im on 28/06/2017.
 * Copyright fir.im
 */
public class ListAdaptor extends BaseAdaptor {

    @Override
    public Class returnedClass() {
        return List.class;
    }

    @Override
    protected Type getTargetType() {
        TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {};
        return typeToken.getType();
    }
}
