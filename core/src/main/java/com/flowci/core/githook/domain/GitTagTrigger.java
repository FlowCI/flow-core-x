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
}
