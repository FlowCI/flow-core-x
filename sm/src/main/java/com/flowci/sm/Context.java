package com.flowci.sm;

import java.util.HashMap;

public class Context extends HashMap<String, Object> {

    public final static String STATUS_CURRENT = "context_current";
    public final static String STATUS_TO = "context_to";
    public final static String ERROR = "context_error";
}
