package com.flowci.core.flow.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public final class WebhookStatus {

    private boolean added;

    private String createdAt;

    private Set<String> events;
}
