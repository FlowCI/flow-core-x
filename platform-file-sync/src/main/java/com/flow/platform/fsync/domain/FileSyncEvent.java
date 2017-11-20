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

package com.flow.platform.fsync.domain;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The file to be sync to client
 *
 * @author yang
 */
public class FileSyncEvent {

    private final static Gson GSON = new GsonBuilder().create();

    public static FileSyncEvent fromBytes(byte[] bytes) {
        return GSON.fromJson(new String(bytes), FileSyncEvent.class);
    }

    /**
     * The original file path in server
     */
    private final String serverPath;

    private final Long size;

    private final String checksum;

    private final FileSyncEventType eventType;

    public FileSyncEvent(String serverPath, Long size, String checksum, FileSyncEventType event) {
        this.serverPath = serverPath;
        this.checksum = checksum;
        this.eventType = event;
        this.size = size;
    }

    public String getServerPath() {
        return serverPath;
    }

    public Long getSize() {
        return size;
    }

    public String getChecksum() {
        return checksum;
    }

    public FileSyncEventType getEventType() {
        return eventType;
    }

    public byte[] toBytes() {
        return GSON.toJson(this).getBytes();
    }
}
