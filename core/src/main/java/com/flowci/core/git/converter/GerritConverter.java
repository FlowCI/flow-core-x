package com.flowci.core.git.converter;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.flowci.core.common.domain.GitSource;
import com.flowci.core.git.domain.GitPatchSetTrigger;
import com.flowci.core.git.domain.GitTrigger;
import com.flowci.core.git.domain.GitTriggerable;
import com.flowci.core.git.domain.GitUser;
import com.flowci.core.git.util.BranchHelper;
import com.flowci.util.StringHelper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Log4j2
@Component("gerritConverter")
public class GerritConverter extends TriggerConverter {

    public static final String Header = "x-origin-url";
    public static final String AllEvent = "all";

    private static final String EventPathsetCreated = "patchset-created";
    private static final String EventChangeMerged = "change-merged";

    private final Map<String, Function<InputStream, GitTrigger>> mapping =
            ImmutableMap.<String, Function<InputStream, GitTrigger>>builder()
                    .put(AllEvent, new AllEventsConverter())
                    .build();

    @Override
    GitSource getGitSource() {
        return GitSource.GERRIT;
    }

    @Override
    Map<String, Function<InputStream, GitTrigger>> getMapping() {
        return mapping;
    }

    private class AllEventsConverter implements Function<InputStream, GitTrigger> {

        @Override
        public GitTrigger apply(InputStream inputStream) {
            try {
                String payload = StringHelper.toString(inputStream);
                Event event = objectMapper.readValue(payload, Event.class);

                if (Objects.equals(EventPathsetCreated, event.type) || Objects.equals(EventChangeMerged, event.type)) {
                    PatchSetEvent pce = objectMapper.readValue(payload, PatchSetEvent.class);
                    return pce.toTrigger();
                }

                log.warn("Unsupported gerrit event {}", event.type);
            } catch (IOException e) {
                log.warn("Unable to parse gerrit payload");
            }

            return null;
        }
    }

    /**
     * Basic event type
     * "type": "ref-updated",
     * "eventCreatedOn": 1642345678
     */
    private static class Event {

        public String type;
    }


    private static class PatchSetEvent implements GitTriggerable {

        @JsonAlias({"uploader", "submitter"})
        public Author author;

        public PatchSet patchSet;

        public Change change;

        public String refName;

        @Override
        public GitTrigger toTrigger() {
            GitPatchSetTrigger t = new GitPatchSetTrigger();
            t.setSource(GitSource.GERRIT);
            t.setEvent(GitTrigger.GitEvent.PATCHSET_UPDATE);

            t.setSubject(change.subject);
            t.setMessage(change.commitMessage);
            t.setProject(change.project);
            t.setChangeId(change.id);
            t.setChangeNumber(change.number);
            t.setChangeUrl(change.url);
            t.setStatus(GitPatchSetTrigger.Status.valueOf(change.status));

            t.setPatchNumber(patchSet.number);
            t.setPatchUrl(change.url + "/" + patchSet.number);
            t.setPatchRevision(patchSet.revision);
            t.setPatchRef(patchSet.ref);

            t.setBranch(BranchHelper.getBranchName(refName));
            t.setCreatedOn(patchSet.createdOn);
            t.setAuthor(author.toGitUser());
            t.setSizeInsertions(patchSet.sizeInsertions);
            t.setSizeDeletions(patchSet.sizeDeletions);

            return t;
        }
    }

    private static class PatchSet {

        public Integer number;

        public String revision;

        public String ref;

        public String createdOn;

        public Integer sizeInsertions;

        public Integer sizeDeletions;
    }

    public static class Change {

        public String project;

        public String branch;

        public String id;

        public Integer number;

        public String subject;

        public String url;

        public String commitMessage;

        public String status;
    }

    private static class Author {

        public String name;

        public String email;

        public String username;

        public GitUser toGitUser() {
            var user = new GitUser();
            user.setName(name);
            user.setEmail(email);
            user.setUsername(username);
            return user;
        }
    }
}
