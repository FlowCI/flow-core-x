package com.flowci.flow.model;

import com.flowci.build.model.Build;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
public class FlowWithBuildStatus {

    @Id
    private Long id;

    private String name;

    private Build.Status lastBuildStatus;
}
