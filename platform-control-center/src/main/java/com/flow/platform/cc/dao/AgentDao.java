package com.flow.platform.cc.dao;

import java.util.Collection;
import java.util.Set;

/**
 * Created by gy@fir.im on 19/05/2017.
 * Copyright fir.im
 */
public interface AgentDao {

    void save(String zone, String agent);

    void reload(String zone, Collection<String> paths);

    void remove(String zone, String agent);

    Set<String> online(String zone);
}
