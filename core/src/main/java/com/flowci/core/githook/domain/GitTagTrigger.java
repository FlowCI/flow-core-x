package com.flowci.core.githook.domain;

import com.flowci.util.StringHelper;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class GitTagTrigger extends GitPushTrigger {

    @Override
    public boolean isSkip() {
        if (!StringHelper.hasValue(getMessage())) {
            return false;
        }

        return getMessage().contains(SkipMessage);
    }

    @Override
    public String getId() {
        return buildId(getSource().name(), getEvent().name(), getRepoId(), getRef());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GitTagTrigger t = (GitTagTrigger) o;
        return Objects.equals(getSource(), t.getSource())
                && Objects.equals(getEvent(), t.getEvent())
                && Objects.equals(getRepoId(), t.getRepoId())
                && Objects.equals(getRef(), t.getRef());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSource(), getEvent(), getRepoId(), getRef());
    }
}
