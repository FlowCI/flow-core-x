package com.flowci.core.githook.domain;

import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * Git trigger to handle gerrit patchset-created event
 */
@Getter
@Setter
public class GitPatchSetTrigger extends GitTrigger {

    private String subject;

    private String message; // commit message

    private String project;

    private String branch;

    private String changeId;

    private Integer changeNumber;

    private String changeUrl;

    private Integer patchNumber;

    private String patchUrl;

    private String revision;

    private String ref;

    private String createdOn;

    private Integer sizeInsertions;

    private Integer sizeDeletions;

    private GitUser author;

    @Override
    public boolean isSkip() {
        return StringHelper.hasValue(message) && message.contains(SkipMessage);
    }

    @Override
    public String getId() {
        return buildId(project, branch, changeId, String.valueOf(patchNumber));
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
