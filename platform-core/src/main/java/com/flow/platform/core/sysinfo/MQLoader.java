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
import com.flow.platform.core.sysinfo.SystemInfo.Status;
import com.flow.platform.core.sysinfo.SystemInfo.Type;
import com.flow.platform.domain.Jsonable;
import com.flow.platform.util.http.HttpClient;
import com.flow.platform.util.http.HttpResponse;
import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

/**
 * MQ loader to load rabbitmq info from rabbitmq management api
 *
 * @author yang
 */
public class MQLoader implements SystemInfoLoader {

    public enum MQGroup implements GroupName {
        RABBITMQ,
    }

    private String mqMgrUrl;

    private String mqMgrUser;

    private String mqMgrPass;

    public MQLoader(String mqMgrUrl, String mqMgrUser, String mqMgrPass) {
        this.mqMgrUrl = mqMgrUrl;
        this.mqMgrUser = mqMgrUser;
        this.mqMgrPass = mqMgrPass;

        if (this.mqMgrUser == null) {
            this.mqMgrUser = "guest";
        }

        if (this.mqMgrPass == null) {
            this.mqMgrPass = "guest";
        }
    }

    @Override
    public SystemInfo load() {
        String url = mqMgrUrl + "/api/overview";
        Map<String, String> authHeader = HttpClient.buildHttpBasicAuthHeader(mqMgrUser, mqMgrPass);

        try {
            HttpResponse<String> response = HttpClient.build(url).get().withHeader(authHeader).bodyAsString();

            if (!response.hasSuccess()) {
                return new SystemInfo(Status.UNKNOWN, Type.MQ);
            }

            RabbitMQOverView mqInfo = RabbitMQOverView.parse(response.getBody(), RabbitMQOverView.class);

            GroupSystemInfo info = new GroupSystemInfo(Status.RUNNING, Type.MQ);
            info.setName(mqInfo.clusterName);
            info.setVersion(mqInfo.version);
            info.put(MQGroup.RABBITMQ, new HashMap<>());

            info.get(MQGroup.RABBITMQ).put("rabbitmq.management.version", mqInfo.managementVersion);
            info.get(MQGroup.RABBITMQ).put("rabbitmq.erlang.version", mqInfo.erlangVersion);
            info.get(MQGroup.RABBITMQ).put("rabbitmq.rates.mode", mqInfo.ratesMode);

            return info;

        } catch (Throwable e) {
            return new SystemInfo(Status.UNKNOWN, Type.MQ);
        }
    }

    private static class RabbitMQOverView extends Jsonable {

        @SerializedName("rabbitmq_version")
        public String version;

        @SerializedName("management_version")
        public String managementVersion;

        @SerializedName("cluster_name")
        public String clusterName;

        @SerializedName("rates_mode")
        public String ratesMode;

        @SerializedName("erlang_version")
        public String erlangVersion;
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
