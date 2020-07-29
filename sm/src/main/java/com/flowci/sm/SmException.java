package com.flowci.sm;

public abstract class SmException {

    public static class TransitionExisted extends IllegalStateException {

        public TransitionExisted () {
            super("Transition already existed");
        }
    }
}
