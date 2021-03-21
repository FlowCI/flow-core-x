package com.flowci.sm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Log4j2
@Setter
@Getter
@RequiredArgsConstructor
public class StateMachine<T extends Context> {

    private final Map<Status, Map<Status, Action<T>>> actions = new HashMap<>();

    private final Map<Status, List<Consumer<T>>> hooksOnTargetStatus = new HashMap<>();

    private final String name;

    private final Executor executor;

    public void addHookActionOnTargetStatus(Consumer<T> action, Status... targets) {
        for (Status target : targets) {
            List<Consumer<T>> actions = hooksOnTargetStatus.computeIfAbsent(target, status -> new LinkedList<>());
            actions.add(action);
        }
    }

    public void add(Transition t, Action<T> action) {
        Map<Status, Action<T>> map = actions.computeIfAbsent(t.getFrom(), k -> new HashMap<>());

        if (map.containsKey(t.getTo())) {
            throw new SmException.TransitionExisted();
        }

        map.put(t.getTo(), action);
    }

    public void executeInExecutor(Status current, Status target, T context) {
        executor.execute(() -> execute(current, target, context));
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

        if (!action.canRun(context)) {
            return;
        }

        try {
            action.accept(context);

            if (context.getTo() != target) {
                return;
            }

            // execute target hook
            hooksOnTargetStatus.computeIfPresent(target, (status, hooks) -> {
                for (Consumer<T> hook : hooks) {
                    hook.accept(context);
                }
                return hooks;
            });

        } catch (Throwable e) {
            log.debug(e);
            action.onException(e, context);
        } finally {
            action.onFinally(context);
        }
    }
}
