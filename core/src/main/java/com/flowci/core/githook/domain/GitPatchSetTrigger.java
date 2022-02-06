package com.flowci.core.githook.domain;

import com.flowci.domain.StringVars;
import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

import static com.flowci.core.common.domain.Variables.Git.*;

/**
 * Git trigger to handle gerrit patchset-created event
 */
@Getter
@Setter
public class GitPatchSetTrigger extends GitTrigger {

    public enum Status {
        NEW,

        MERGED
    }

    private Status status;

    private String subject;

    private String message; // commit message

    private String project;

    private String branch;

    private String changeId;

    private Integer changeNumber;

    private String changeUrl;

    private Integer patchNumber;

    private String patchUrl;

    private String patchRef;

    private String patchRevision;

    private String createdOn;

    private Integer sizeInsertions;

    private Integer sizeDeletions;

    private GitUser author;

    @Override
    public StringVars toVariableMap() {
        var map = super.toVariableMap();
        map.put(PATCHSET_SUBJECT, subject);
        map.put(PATCHSET_MESSAGE, message);
        map.put(PATCHSET_PROJECT, project);
        map.put(PATCHSET_BRANCH, branch);
        map.put(PATCHSET_CHANGE_ID, changeId);
        map.put(PATCHSET_CHANGE_NUM, String.valueOf(changeNumber));
        map.put(PATCHSET_CHANGE_URL, changeUrl);
        map.put(PATCHSET_CHANGE_STATUS, status.name());
        map.put(PATCHSET_PATCH_NUM, String.valueOf(patchNumber));
        map.put(PATCHSET_PATCH_URL, patchUrl);
        map.put(PATCHSET_PATCH_REF, patchRef);
        map.put(PATCHSET_PATCH_REVISION, patchRevision);
        map.put(PATCHSET_CREATE_TIME, createdOn);
        map.put(PATCHSET_INSERT_SIZE, String.valueOf(sizeInsertions));
        map.put(PATCHSET_DELETE_SIZE, String.valueOf(sizeDeletions));
        map.put(PATCHSET_AUTHOR, author.getEmail());

        if (status == Status.MERGED) {
            map.put(BRANCH, branch);
        }

        if (status == Status.NEW) {
            map.put(BRANCH, patchRef);
        }

        return map;
    }

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
