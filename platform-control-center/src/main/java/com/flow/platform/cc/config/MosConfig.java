package com.flow.platform.cc.config;

import com.flow.platform.util.logger.Logger;
import com.flow.platform.util.mos.MosClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

import static com.flow.platform.util.mos.MosConfig.*;

/**
 * Meituan cloud configuration
 *
 * Created by gy@fir.im on 15/06/2017.
 * Copyright fir.im
 */
@Configuration
public class MosConfig {

    private final static Logger LOGGER = new Logger(MosConfig.class);

    @Value("${mos.key}")
    private String apiKey;

    @Value("${mos.secret}")
    private String apiSecret;

    @Value("${mos.net_id}")
    private String netId;

    @Value("${mos.ssh_key_name}")
    private String sshKeyName;

    @Value("${mos.zone_id}")
    private String zoneId;

    @Value("${mos.instance_type}")
    private String instanceType;

    @Value("${mos.duration}")
    private String duration;

    @Value("${mos.group_id}")
    private String groupId;

    @PostConstruct
    public void init() {
        LOGGER.trace("NetId: %s", netId);
        LOGGER.trace("SSH key name: %s", sshKeyName);
        LOGGER.trace("ZoneId: %s", zoneId);
        LOGGER.trace("InstanceType: %s", instanceType);
        LOGGER.trace("Duration: %s", duration);
        LOGGER.trace("GroupId: %s", groupId);
    }

    @Bean
    public MosClient mosClient() throws Throwable {
        DEFAULT_NET_ID = netId;
        DEFAULT_SSH_KEY_NAME = sshKeyName;
        DEFAULT_ZONE_ID = zoneId;
        DEFAULT_INSTANCE_TYPE = instanceType;
        DEFAULT_DURATION = duration;
        DEFAULT_GROUP_ID = groupId;

        return new MosClient(apiKey, apiSecret);
    }
}
