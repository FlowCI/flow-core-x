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

package com.flow.platform.api.security;

import com.flow.platform.api.domain.permission.Actions;
import com.flow.platform.api.domain.user.Action;

/**
 * @author yang
 */
public interface UserSecurityService {

    /**
     * Verify the action is accessible for user
     */
    boolean canAccess(String email, Action action);

    /**
     * Get action from Actions enum
     */
    Action getAction(Actions actionName);
}
