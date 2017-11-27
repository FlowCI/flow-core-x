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

package com.flow.platform.plugin.event;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yh@firim
 */
public abstract class AbstractEvent {

    //TODO: change abstractEvent to ApplicationContext

    protected List<PluginListener> listeners = new LinkedList<>();

    protected <T> void dispatchEvent(T t, String tag, Path path, String pluginName) {
        for (PluginListener listener : listeners) {
            listener.call(t, tag, path, pluginName);
        }
    }
}
