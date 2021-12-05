package com.flowci.core.githook.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
public final class GitCommit {

    private String id;

    private String message;

    private String time;

    private String url;

    private GitUser author;
}
