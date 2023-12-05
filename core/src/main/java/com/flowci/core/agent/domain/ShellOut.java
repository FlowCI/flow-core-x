package com.flowci.core.agent.domain;

import com.flowci.core.job.domain.Executed;
import com.flowci.common.domain.StringVars;
import com.flowci.common.domain.Vars;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
public final class ShellOut implements Executed, CmdOut {

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
