package com.flow.platform.cc.dao;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by gy@fir.im on 19/05/2017.
 * Copyright fir.im
 */

@Component(value = "agentDao")
public class AgentDaoImpl implements AgentDao {

    // zone - agent set
    private final Map<String, Set<String>> onlineAgents = new ConcurrentHashMap<>(10);
    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public Set<String> online(String zone) {
        return onlineAgents.computeIfAbsent(zone, k -> new HashSet<>(100));
    }

    @Override
    public void save(String zone, String agent) {
        lock.lock();
        try {
            Set<String> sets = onlineAgents.computeIfAbsent(zone, k -> new HashSet<>(100));
            sets.add(agent);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void reload(String zone, Collection<String> agents) {
        lock.lock();
        try {
            Set<String> sets = onlineAgents.computeIfAbsent(zone, k -> new HashSet<>(100));
            sets.clear();
            sets.addAll(agents);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(String zone, String agent) {
        lock.lock();
        try {
            onlineAgents.get(zone).remove(agent);
        } finally {
            lock.unlock();
        }
    }
}
