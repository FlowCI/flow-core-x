package com.flowci.core.git.domain;

import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(of = "id")
@Accessors(chain = true)
public class GitCommit {

    private String id;

    private String message;

    private String time;

    private String url;

    private GitUser author;
}
