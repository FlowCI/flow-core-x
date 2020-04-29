package com.flowci.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Log4j2
@Setter
@Getter
@RequiredArgsConstructor
public class StateMachine<T extends Context> {

    private final Map<Status, Map<Status, Action<T>>> actions = new HashMap<>();

    private final String name;

    private Lockable lockable;

    private boolean isSync;

    public void add(Transition t, Action<T> action) {
        Map<Status, Action<T>> map = actions.computeIfAbsent(t.getFrom(), k -> new HashMap<>());

        if (map.containsKey(t.getTo())) {
            throw new IllegalStateException("Transition already existed");
        }

        map.put(t.getTo(), action);
    }

    public void execute(Status current, Status target, T context) {
        context.setCurrent(current);
        context.setTo(target);

        Map<Status, Action<T>> actionMap = actions.get(current);
        if (Objects.isNull(actionMap)) {
            log.warn("No status from {}", current.getName());
            return;
        }

        Action<T> action = actionMap.get(target);
        if (Objects.isNull(action)) {
            log.warn("No status from {} to {}", current.getName(), target.getName());
            return;
        }

        if (isSync) {
            Objects.requireNonNull(lockable, "Lockable instance is null");
            lockable.lock(name);
        }

        try {
            action.accept(context);
        } finally {
            // release lock
            if (isSync) {
                lockable.unlock(name);
            }
        }
    }
}
