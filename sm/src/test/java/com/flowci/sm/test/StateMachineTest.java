package com.flowci.sm.test;

import com.flowci.sm.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

public class StateMachineTest {

    private final Status A = new Status("A");
    private final Status B = new Status("B");
    private final Status C = new Status("C");

    private final Transition AtoB = new Transition(A, B);
    private final Transition AtoC = new Transition(A, C);

    private final StateMachine<TestContext> sm = new StateMachine<>("TEST", null);

    private static class TestContext extends Context {

    }

    @Test
    public void should_skip_after_event_if_state_changed() {
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

        sm.execute(A, B, new TestContext());

        Assert.assertTrue(shouldExecute.get());
        Assert.assertTrue(shouldNotExecute.get());
    }
}
