package com.flowci.sm.test;

import com.flowci.sm.*;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StateMachineTest {

    private final Status A = new Status("A");
    private final Status B = new Status("B");
    private final Status C = new Status("C");

    private final Transition AtoB = new Transition(A, B);
    private final Transition AtoC = new Transition(A, C);
    private final Transition BtoC = new Transition(B, C);

    private final StateMachine<TestContext> sm = new StateMachine<>("TEST", null);

    private static class TestContext extends Context {

    }

    @Test
    void should_call_hook_only_once_when_current_state_changed() {
        sm.add(AtoC, new Action<TestContext>() {
            @Override
            public void accept(TestContext ctx) throws Exception {
                sm.execute(B, C, ctx);
            }
        });

        AtomicReference<Boolean> shouldExecute = new AtomicReference<>(Boolean.FALSE);
        sm.add(BtoC, new Action<TestContext>() {
            @Override
            public void accept(TestContext ctx) throws Exception {
                shouldExecute.set(Boolean.TRUE);
            }
        });

        AtomicInteger shouldCallOnceOnly = new AtomicInteger(0);
        sm.addHookActionOnTargetStatus(testContext -> shouldCallOnceOnly.getAndIncrement(), C);

        sm.execute(A, C, new TestContext());

        assertTrue(shouldExecute.get());
        assertEquals(1, shouldCallOnceOnly.get());
    }

    @Test
    void should_skip_target_hook_after_event_if_target_state_changed() {
        sm.add(AtoB, new Action<TestContext>() {
            @Override
            public void accept(TestContext ctx) throws Exception {
                // switch to C
                sm.execute(A, C, ctx);
            }
        });

        AtomicReference<Boolean> shouldExecute = new AtomicReference<>(Boolean.FALSE);
        sm.add(AtoC, new Action<TestContext>() {
            @Override
            public void accept(TestContext ctx) throws Exception {
                shouldExecute.set(Boolean.TRUE);
            }
        });

        AtomicReference<Boolean> shouldNotExecute = new AtomicReference<>(Boolean.TRUE);
        sm.addHookActionOnTargetStatus(testContext -> shouldNotExecute.set(Boolean.FALSE), B);

        AtomicReference<Boolean> shouldExecuteOnHook = new AtomicReference<>(Boolean.FALSE);
        sm.addHookActionOnTargetStatus(testContext -> shouldExecuteOnHook.set(Boolean.TRUE), C);

        sm.execute(A, B, new TestContext());

        assertTrue(shouldExecute.get());
        assertTrue(shouldNotExecute.get());
        assertTrue(shouldExecuteOnHook.get());
    }
}
