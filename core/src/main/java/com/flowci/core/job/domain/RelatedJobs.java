package com.flowci.core.job.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter
@Setter
@Document("job_related_jobs")
public class RelatedJobs {

    @Id
    private String id;

    @Indexed(name = "index_git_event_id", unique = true)
    private String gitEventId;

    private List<JobDesc> jobs;
}
