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

package com.flow.platform.cmd;

/**
 * @author gy@fir.im
 */
public final class Log {

    public enum Type {
        STDOUT,
        STDERR,
    }

    public Log(Type type, String content) {
        this.type = type;
        this.content = content;
    }

    public Log(Type type, String content, Integer count) {
        this.type = type;
        this.content = content;
        this.number = count;
    }

    public enum Category {
        DEFAULT,
        OTHER
    }

    private Category category = Category.DEFAULT;

    private Type type;

    private String content;

    private Integer number;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Type getType() {
        return type;
    }

    public String getContent() {
        return content;
    }

    public Integer getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "Log{" +
            "type=" + type +
            ", content='" + content + '\'' +
            '}';
    }
}
