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

package com.flow.platform.cc.config;

import com.flow.platform.domain.Zone;
import com.flow.platform.util.Logger;
import com.flow.platform.util.ObjectUtil;
import com.flow.platform.util.zk.ZKClient;
import com.google.common.base.Strings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * @author yang
 */
@Configuration
public class ZooKeeperConfig {

    private final static Logger LOGGER = new Logger(ZooKeeper.class);

    /**
     * Zone dynamic property naming rule
     * - First %s is zone name
     * - Seconds %s is property name xx_yy_zz from Zone.getXxYyZz
     */
    private final static String ZONE_PROPERTY_NAMING = "zone.%s.%s";

    @Value("${zk.host}")
    private String host;

    @Value("${zk.timeout}")
    private Integer timeout;

    @Value("${zk.node.root}")
    private String rootNodeName;

    @Value("${zk.node.zone}")
    private String zonesDefinition;

    @Autowired
    private Environment env;

    @PostConstruct
    public void init() {
        LOGGER.trace("Host: %s", host);
        LOGGER.trace("Root node: %s", rootNodeName);
        LOGGER.trace("Zones: %s", zonesDefinition);
    }

    @Bean
    public ZKClient zkClient() {
        ZKClient zkClient = new ZKClient(host);
        if (zkClient.start()) {
            LOGGER.trace("Zookeeper been connected at: %s", host);
        } else {
            throw new RuntimeException(String.format("Fail to connect zookeeper server: %s", host));
        }
        return zkClient;
    }

    @Bean
    public List<Zone> defaultZones() {
        return loadZones(zonesDefinition);
    }

    @PreDestroy
    public void destroy() {
        // ignore, zkClient closed in ZooKeeperService
    }

    /**
     * Load default zones from app.properties
     */
    private List<Zone> loadZones(String zonesDefinition) {
        String[] zoneAndProviderList = zonesDefinition.split(";");
        List<Zone> zones = new ArrayList<>(zoneAndProviderList.length);

        for (String zoneName : zoneAndProviderList) {
            Zone zone = new Zone();
            zone.setName(zoneName);
            fillZoneProperties(zone);
            zones.add(zone);
        }

        return zones;
    }

    /**
     * Dynamic fill zone properties from app.properties to Zone instance
     */
    private void fillZoneProperties(Zone emptyZone) {
        Field[] fields = ObjectUtil.getFields(Zone.class);
        for (Field field : fields) {
            String flatted = ObjectUtil.convertFieldNameToFlat(field.getName());
            String valueFromConfig = env.getProperty(String.format(ZONE_PROPERTY_NAMING, emptyZone.getName(), flatted));

            // assign value to bean
            if (!Strings.isNullOrEmpty(valueFromConfig)) {
                ObjectUtil.assignValueToField(field, emptyZone, valueFromConfig);
            }
        }
    }
}
