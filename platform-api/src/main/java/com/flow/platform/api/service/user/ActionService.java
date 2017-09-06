/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.flow.platform.api.service.user;

import com.flow.platform.api.domain.request.ActionParam;
import com.flow.platform.api.domain.user.Action;
import java.util.List;

/**
 * @author lhl
 */
public interface ActionService {

    /**
     * Find action by name
     *
     * @throws com.flow.platform.core.exception.IllegalParameterException if action name not existed
     */
    Action find(String name);

    /**
     * Create action by name, alias, and desc
     *
     * @throws com.flow.platform.core.exception.IllegalParameterException if action name not presented or existed
     */
    Action create(Action action);

    /**
     * Update action alias or desc
     */
    Action update(String name, ActionParam body);

    /**
     * Delete action
     *
     * @param name action name
     * @throws com.flow.platform.core.exception.IllegalParameterException if action name doesn't existed
     * @throws com.flow.platform.core.exception.IllegalStatusException if has role assigned to action
     */
    void delete(String name);

    /**
     * List all action
     */
    List<Action> list();
}
