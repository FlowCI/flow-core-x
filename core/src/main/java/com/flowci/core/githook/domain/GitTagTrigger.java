package com.flowci.core.githook.domain;

import com.flowci.util.StringHelper;
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
}
