package com.flowci.core.common.manager;

import com.flowci.common.helper.ObjectsHelper;
import com.flowci.common.helper.PatternHelper;
import com.flowci.common.helper.StringHelper;
import com.flowci.core.config.event.GetConfigEvent;
import com.flowci.core.secret.event.GetSecretEvent;
import com.flowci.domain.Input;
import com.flowci.domain.VarType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class VarManager {

    @Autowired
    private SpringEventManager eventManager;

    public boolean verify(Input input, String value) {
        if (input.isRequired() && StringHelper.isEmpty(value)) {
            return false;
        }

        if (!input.isRequired() && !StringHelper.hasValue(value)) {
            return true;
        }

        return verify(input.getType(), value);
    }

    public boolean verify(VarType type, String value) {
        switch (type) {
            case INT:
                return ObjectsHelper.tryParseInt(value);

            case BOOL:
                return Objects.equals(value, "true") || Objects.equals(value, "false");

            case HTTP_URL:
                return PatternHelper.isWebURL(value);

            case GIT_URL:
                return PatternHelper.isGitURL(value);

            case EMAIL:
                return PatternHelper.isEmail(value);

            case CONFIG:
                return !eventManager.publish(new GetConfigEvent(this, value)).hasError();

            case SECRET:
                return !eventManager.publish(new GetSecretEvent(this, value)).hasError();
        }

        return true;
    }
}
