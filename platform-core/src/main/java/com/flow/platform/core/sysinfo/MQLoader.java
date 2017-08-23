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

package com.flow.platform.core.sysinfo;

import com.flow.platform.core.exception.IllegalURLException;
import com.google.common.base.Strings;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * MQ loader to load rabbitmq info from rabbitmq management api
 *
 * @author yang
 */
public class MQLoader implements SystemInfoLoader {

    public enum MQGroup implements GroupName {
        RABBITMQ,
    }

    private final MQURL mqHostUrl;

    /**
     * For url format, reference on https://www.rabbitmq.com/uri-spec.html
     */
    public MQLoader(String mqHostUrl) {
        this.mqHostUrl = new MQURL(mqHostUrl);
    }

    @Override
    public SystemInfo load() {

        return null;
    }

    public static class MQURL {

        private String protocol;

        private String user;

        private String pass;

        private String host;

        private Integer port;

        private String vHost;

        public MQURL(String mqHost) {
            parse(mqHost);
            decode();
        }

        public String getProtocol() {
            return protocol;
        }

        public String getUser() {
            return user;
        }

        public String getPass() {
            return pass;
        }

        public String getHost() {
            return host;
        }

        public Integer getPort() {
            return port;
        }

        public String getvHost() {
            return vHost;
        }

        private void parse(String mqHost) {
            // find protocol
            int colonIndex = mqHost.indexOf("://");
            if (colonIndex == -1) {
                throw new IllegalURLException("Protocol not defined");
            }
            protocol = mqHost.substring(0, colonIndex);

            // reset mq host string
            mqHost = mqHost.substring(colonIndex + 3);

            // find user and pass
            int atIndex = mqHost.indexOf('@');
            if (atIndex > -1) {
                String userInfo = mqHost.substring(0, atIndex);
                int userColonIndex = userInfo.indexOf(':');

                // user info contains pass
                if (userColonIndex > -1) {
                    user = userInfo.substring(0, userColonIndex);
                    pass = userInfo.substring(userColonIndex + 1);
                }

                // user info without pass
                else {
                    user = userInfo;
                }
            }

            // find vhost info
            int lastSlashIndex = mqHost.lastIndexOf('/');
            if (lastSlashIndex > -1) {
                vHost = mqHost.substring(lastSlashIndex + 1);
            }

            // find host info
            try {
                String hostInfo = lastSlashIndex == -1
                    ? mqHost.substring(atIndex + 1)
                    : mqHost.substring(atIndex + 1, lastSlashIndex);

                if (Strings.isNullOrEmpty(hostInfo)) {
                    return;
                }

                int hostColonIndex = hostInfo.indexOf(':');
                if (hostColonIndex > -1) {
                    host = hostInfo.substring(0, hostColonIndex);
                    port = Integer.parseInt(hostInfo.substring(hostColonIndex + 1));
                } else {
                    host = hostInfo;
                }
            } catch (StringIndexOutOfBoundsException ignore) {
            }
        }

        private void decode() {
            user = decode(user);
            pass = decode(pass);
            host = decode(host);
            vHost = decode(vHost);
        }

        private String decode(String source) {
            if (Strings.isNullOrEmpty(source)) {
                return source;
            }

            try {
                return URLDecoder.decode(source, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
    }
}
