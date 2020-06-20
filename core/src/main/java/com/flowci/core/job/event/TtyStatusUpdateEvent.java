package com.flowci.core.job.event;

import com.flowci.core.agent.domain.TtyCmd;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class TtyStatusUpdateEvent extends ApplicationEvent {

    private final TtyCmd.Out out;

    public TtyStatusUpdateEvent(Object source, TtyCmd.Out out) {
        super(source);
        this.out = out;
    }
}
