package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import io.hypersistence.utils.hibernate.type.array.LongArrayType;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

import java.util.Map;

/**
 * Job refer to Step from YAML
 */
@Data
@Entity
@Table(name = "jobs")
@EqualsAndHashCode(of = "id", callSuper = false)
public class Job extends EntityBase {

    public enum Status {
        CREATED, // init status
        QUEUED, // been put to job queue and waiting for agent
        ASSIGNED, // assigned to an agent
        RUNNING, // agent start to execute the flow
        CANCELLING, // will be cancelled, but waiting for response from agent
        SUCCESS,
        FAILURE,
        CANCELLED,
        TIMEOUT
    }

    @Id
    private Long id;

    private Long buildId;

    private String name;

    /**
     * Dependency job id list
     */
    @org.hibernate.annotations.Type(LongArrayType.class)
    private Long[] dependencies;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Nullable
    @org.hibernate.annotations.Type(StringArrayType.class)
    private String[] agentTags;

    /**
     * Assigned agent id
     */
    @Nullable
    private Long agentId;

    /**
     * All input variables
     * value assigned when agent assigned, need to take build context
     */
    @Nullable
    @Type(JsonType.class)
    private Variables variables;

    /**
     * Environment variables with value
     */
    @Nullable
    @Type(JsonType.class)
    private Map<String, String> output;
}