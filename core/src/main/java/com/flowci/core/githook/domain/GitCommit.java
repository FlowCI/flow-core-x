package com.flowci.core.githook.domain;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@EqualsAndHashCode(of = "id")
public final class GitCommit {

    private String id;

    private String message;

    private String time;

    private String url;

    private GitUser author;
}
