package com.flowci.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Setter
@Getter
@RequiredArgsConstructor
public class StateMachine {

    public final static Map<Status, Map<Status, Action>> actions = new HashMap<>();

    private final String name;

    private Lockable lockable;

    private boolean isSync;

    public void add(Transition t, Action action) {
        Map<Status, Action> map = actions.computeIfAbsent(t.getFrom(), k -> new HashMap<>());

        if (map.containsKey(t.getTo())) {
            throw new IllegalStateException("Transition already existed");
        }

        map.put(t.getTo(), action);
    }

    public void execute(Status current, Status target, Context context) {
        Action action = actions.get(current).get(target);

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
