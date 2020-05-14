package com.flowci.sm;

import lombok.Data;

@Data
public class Transition {

    private final Status from;

    private final Status to;
}
