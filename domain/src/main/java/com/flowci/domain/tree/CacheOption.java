package com.flowci.domain.tree;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "key")
public class CacheOption implements Serializable {

    private String key;

    private List<String> paths;
}
