package com.flowci.core.job.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.InputStream;
import java.util.List;

@Getter
@Setter
@Document("job_cache")
@CompoundIndex(name = "index_flow_key", def = "{'flowId' : 1, 'key': 1}", unique = true)
public class JobCache extends Mongoable {

    private String flowId;

    private String jobId;

    private String key;

    /**
     * Full path with b64 encoded based on workspace
     * ex: a/b/c.zip --> b64xxx.zip
     */
    private List<String> files;

    @JsonIgnore
    @Transient
    private InputStream src;
}
