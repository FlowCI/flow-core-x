package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import com.flowci.common.model.Variables;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.io.Serializable;
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

    @Data
    @Embeddable
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Id implements Serializable {
        private Long buildId;
        private String name;
    }

    @EmbeddedId
    private Id id;

    /**
     * Next step name list
     */
    @Nullable
    @org.hibernate.annotations.Type(StringArrayType.class)
    private String[] next;

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