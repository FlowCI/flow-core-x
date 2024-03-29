package com.flowci.core.git.domain;

import com.flowci.common.helper.StringHelper;
import lombok.Getter;
import lombok.Setter;

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
