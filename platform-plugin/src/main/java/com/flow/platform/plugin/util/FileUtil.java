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

package com.flow.platform.plugin.util;

import com.flow.platform.plugin.exception.PluginException;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;

/**
 * @author yh@firim
 */
public class FileUtil {

    private final static Gson GSON = new GsonBuilder().create();

    synchronized public static <T> void write(T object, Path path) {
        try {
            Files.write(GSON.toJson(object).getBytes(), path.toFile());
        } catch (IOException e) {
            throw new PluginException("IOException: " + e.getMessage());
        }
    }

    synchronized public static <T> T read(Type clazz, Path path) {
        if (!path.toFile().exists()) {
            return null;
        }

        try {
            return GSON.fromJson(Files.toString(path.toFile(), Charsets.UTF_8), clazz);
        } catch (IOException e) {
            throw new PluginException("IOException: " + e.getMessage());
        }
    }
}
