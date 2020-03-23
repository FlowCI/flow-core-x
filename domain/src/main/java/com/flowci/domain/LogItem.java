/*
 * Copyright 2018 flow.ci
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

package com.flowci.domain;

import java.io.Serializable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
public final class LogItem implements Serializable {

    public static LogItem of(Type type, String content) {
        return new LogItem(type, content);
    }

    public enum Type {
        STDOUT,
        STDERR,
    }

    public static final char SPLITTER = '#';

    private String cmdId;

    private Type type;

    private String content;

    private long number;

    private LogItem(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    /**
     * To byte array ex: cmdid#type#number#content
     *
     */
    public byte[] toBytes() {
        return toString().getBytes();
    }

    @Override
    public String toString() {
        return cmdId + SPLITTER + type + SPLITTER + number + SPLITTER + content;
    }
}
