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

package com.flow.platform.api.initializers;

import com.flow.platform.core.context.ContextEvent;

/**
 * @author yang
 */
public abstract class Initializer implements ContextEvent {

    public static boolean ENABLED = true;

    @Override
    public void start() {
        if (ENABLED) {
            doStart();
        }
    }

    @Override
    public void stop() {

    }

    abstract void doStart();
}
