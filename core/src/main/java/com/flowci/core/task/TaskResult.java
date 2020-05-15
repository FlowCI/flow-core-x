package com.flowci.core.task;

import com.flowci.core.common.domain.Mongoable;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document("local_task_result")
public final class TaskResult extends Mongoable {

    @NonNull
    private String name;

    @NonNull
    @Indexed(name = "index_task_job_id")
    private String jobId;

    private int exitCode = -1;

    private String containerId;

    private String err;

    public boolean hasContainerId() {
        return StringHelper.hasValue(containerId);
    }
}
