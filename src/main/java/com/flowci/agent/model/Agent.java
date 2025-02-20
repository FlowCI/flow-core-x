package com.flowci.agent.model;

import com.flowci.common.model.EntityBase;
import io.hypersistence.utils.hibernate.type.array.StringArrayType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false, of = {"id"})
@Entity
@Table(name = "agents")
public class Agent extends EntityBase {

    public enum Status {
        OFFLINE,
        ONLINE_BUSY,
        ONLINE_IDLE,
        DISABLED,
    }

    @Id
    private Long id;

    private String alias;

    // generated
    private String token;

    // updated when agent online
    private String os;

    @org.hibernate.annotations.Type(StringArrayType.class)
    private String[] tags;

    @Enumerated(EnumType.STRING)
    private Status status;
}
