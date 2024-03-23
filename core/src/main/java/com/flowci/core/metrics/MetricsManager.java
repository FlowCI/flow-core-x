package com.flowci.core.metrics;

import com.flowci.core.job.event.JobFinishedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@AllArgsConstructor
public class MetricsManager {

    private final MeterRegistry meterRegistry;

    @EventListener(JobFinishedEvent.class)
    public void onJobFinished(JobFinishedEvent e) {
        Counter.builder("num_of_finished_job")
                .description("num of finished job")
                .register(meterRegistry)
                .increment();

        log.debug("metrics: num_of_finished_job increment");
    }
}
