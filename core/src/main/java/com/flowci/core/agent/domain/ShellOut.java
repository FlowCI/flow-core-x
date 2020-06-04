package com.flowci.core.agent.domain;

import com.flowci.core.job.domain.Executed;
import com.flowci.domain.StringVars;
import com.flowci.domain.Vars;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public final class ShellOut implements Executed {

    private String id;

    private int processId;

    private String containerId;

    private Executed.Status status;

    private Integer code;

    private Vars<String> output = new StringVars();

    private Date startAt;

    private Date finishAt;

    private String error;

    private long logSize;

}
