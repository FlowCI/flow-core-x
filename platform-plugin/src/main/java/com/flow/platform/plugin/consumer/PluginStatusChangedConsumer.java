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

package com.flow.platform.plugin.consumer;

import com.flow.platform.plugin.domain.PluginStatus;
import com.flow.platform.plugin.event.PluginListener;
import com.flow.platform.util.Logger;
import java.nio.file.Path;

/**
 * @author yh@firim
 */
public class PluginStatusChangedConsumer implements PluginListener<PluginStatus> {

    private final static Logger LOGGER = new Logger(PluginStatusChangedConsumer.class);

    @Override
    public void call(PluginStatus pluginStatus, String tag, Path path) {
        LOGGER.traceMarker("PluginStatusChangedConsumer", "Incoming Message PluginStatus:"
            + pluginStatus.toString()
            + ", Tag is "
            + tag
            + ", path is"
            + path.toString());
    }
}
