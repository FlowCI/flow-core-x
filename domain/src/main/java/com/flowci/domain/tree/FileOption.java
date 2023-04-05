package com.flowci.domain.tree;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "name")
public abstract class FileOption implements Serializable {

    private String name;

    private List<String> paths;
}
