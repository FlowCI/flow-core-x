package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Setter
@Getter
@Document("executed_local_task")
@CompoundIndex(
        name = "index_job_id_and_name",
        def = "{'jobId': 1, 'name': 1}",
        unique = true
)
public final class ExecutedLocalTask implements Executed {

    private String id;

    @NonNull
    @Indexed(name = "index_task_job_id")
    private String jobId;

    @NonNull
    private String name;

    private Status status = Status.PENDING;

    private Integer code;

    private String containerId;

    private String error;

    private Date startAt;

    private Date finishAt;

    @JsonIgnore
    public boolean hasContainerId() {
        return StringHelper.hasValue(containerId);
    }
}
