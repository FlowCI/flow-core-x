package com.flowci.build.model;

import com.flowci.common.model.EntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Entity
@Table(name = "builds_yaml")
@EqualsAndHashCode(callSuper = false, of = "id")
public class BuildYaml extends EntityBase {

    @Id
    private Long id;

    private String yaml;
}
