package io.github.ymrsl.firstpersonfoodeating.client.script.statemachine;

import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnimationStateMachine<T extends AnimationStateContext> {
    private List<AnimationState<T>> currentStates;
    protected T context;
    private Supplier<Iterable<? extends AnimationState<T>>> statesSupplier;
    private final @Nonnull FoodAnimationController animationController;
    protected long exitingTime = -1;

    public AnimationStateMachine(@Nonnull FoodAnimationController animationController) {
        this.animationController = Objects.requireNonNull(animationController);
    }

    public void update(float deltaSeconds) {
        if (context != null && currentStates != null) {
            currentStates.forEach(state -> state.update(context));
        }
        animationController.update(deltaSeconds);
    }

    public void trigger(String condition) {
        if (context == null || currentStates == null) {
            return;
        }
        ListIterator<AnimationState<T>> iterator = currentStates.listIterator();
        while (iterator.hasNext()) {
            AnimationState<T> state = iterator.next();
            AnimationState<T> nextState = state.transition(context, condition);
            if (nextState != null) {
                state.exitAction(context);
                iterator.set(nextState);
                nextState.entryAction(context);
            }
        }
    }

    public void initialize() {
        if (context == null) {
            throw new IllegalStateException("Context must not be null before initialization");
        }
        if (currentStates != null) {
            throw new IllegalStateException("State machine already initialized");
        }
        this.currentStates = new LinkedList<>();
        if (statesSupplier == null) {
            return;
        }
        Iterable<? extends AnimationState<T>> states = statesSupplier.get();
        for (AnimationState<T> state : states) {
            currentStates.add(state);
            state.entryAction(context);
        }
    }

    public void exit() {
        checkNullPointer();
        currentStates.forEach(state -> state.exitAction(context));
        this.currentStates = null;
    }

    public void setExitingTime(long keepTimeMs) {
        this.exitingTime = System.currentTimeMillis() + keepTimeMs;
    }

    public long getExitingTime() {
        return exitingTime;
    }

    public @Nonnull FoodAnimationController getAnimationController() {
        return animationController;
    }

    public boolean isInitialized() {
        return currentStates != null;
    }

    public @Nullable T getContext() {
        return context;
    }

    public void processContextIfExist(Consumer<T> consumer) {
        if (context != null) {
            consumer.accept(context);
        }
    }

    public void setContext(@Nonnull T context) {
        AnimationStateMachine<?> stateMachine = context.getStateMachine();
        if (stateMachine != null && stateMachine != this) {
            throw new IllegalStateException("Context already used by another state machine");
        }
        if (currentStates != null) {
            throw new IllegalStateException("State machine already initialized, call exit() first");
        }
        if (this.context != null) {
            this.context.setStateMachine(null);
        }
        context.setStateMachine(this);
        this.context = context;
    }

    public void setStatesSupplier(Supplier<Iterable<? extends AnimationState<T>>> statesSupplier) {
        this.statesSupplier = statesSupplier;
    }

    private void checkNullPointer() {
        if (context == null) {
            throw new IllegalStateException("Context has not been initialized");
        }
        if (currentStates == null) {
            throw new IllegalStateException("State machine has not been initialized");
        }
    }
}
