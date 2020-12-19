package com.flowci.core.job.domain;

import com.google.common.collect.ImmutableSet;
import lombok.Getter;

import java.util.Date;
import java.util.Set;

public interface Executed {

    Set<Status> FailureStatus = ImmutableSet.of(
            Status.EXCEPTION,
            Status.KILLED,
            Status.TIMEOUT
    );

    Set<Status> OngoingStatus = ImmutableSet.of(
            Status.PENDING,
            Status.WAITING_AGENT,
            Status.RUNNING
    );

    Set<Status> SuccessStatus = ImmutableSet.of(
            Status.SUCCESS,
            Status.SKIPPED
    );

    Set<Status> FinishStatus = ImmutableSet.of(
            Status.SUCCESS,
            Status.SKIPPED,
            Status.EXCEPTION,
            Status.KILLED,
            Status.TIMEOUT
    );

    enum Status {

        PENDING(-1),

        WAITING_AGENT(0),

        RUNNING(1),

        SUCCESS(2),

        SKIPPED(2),

        EXCEPTION(3),

        KILLED(3),

        TIMEOUT(4);

        @Getter
        private final Integer level;

        Status(Integer level) {
            this.level = level;
        }
    }

    Integer getCode();

    Status getStatus();

    String getContainerId();

    Date getStartAt();

    Date getFinishAt();

    String getError();
}
