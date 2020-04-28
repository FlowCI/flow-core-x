package com.flowci.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.flowci.sm.Context.STATUS_CURRENT;
import static com.flowci.sm.Context.STATUS_TO;

@Log4j2
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
        context.put(STATUS_CURRENT, current);
        context.put(STATUS_TO, target);

        Map<Status, Action> actionMap = actions.get(current);
        if (Objects.isNull(actionMap)) {
            log.warn("No status from {}", current.getName());
            return;
        }

        Action action = actionMap.get(target);
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
