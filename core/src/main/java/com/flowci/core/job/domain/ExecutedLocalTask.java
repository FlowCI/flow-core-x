package com.flowci.core.job.domain;

import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Setter
@Getter
@Document("local_task_result")
public final class ExecutedLocalTask {

    private String id;

    @NonNull
    private String name;

    @NonNull
    @Indexed(name = "index_task_job_id")
    private String jobId;

    private int code = -1;

    private String containerId;

    private String err;

    public boolean hasContainerId() {
        return StringHelper.hasValue(containerId);
    }
}
