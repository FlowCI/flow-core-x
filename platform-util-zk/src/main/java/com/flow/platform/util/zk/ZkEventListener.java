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

package com.flow.platform.util.zk;

import org.apache.zookeeper.WatchedEvent;

/**
 * @author gy@fir.im
 */
public interface ZkEventListener {

    /**
     * On zk client connected to server
     *
     * @param event zk raw event
     * @param path zk node path
     */
    void onConnected(WatchedEvent event, String path);

    /**
     * On receive zk data changed
     *
     * @param event zk raw event
     * @param raw raw byte array
     */
    void onDataChanged(WatchedEvent event, byte[] raw);

    /**
     * On data changed been executed
     *
     * @param event zk raw event
     */
    void afterOnDataChanged(WatchedEvent event);

    /**
     * On node deleted
     *
     * @param event zk raw event
     */
    void onDeleted(WatchedEvent event);
}
