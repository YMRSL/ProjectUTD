package io.github.ymrsl.firstpersonfoodeating.client.script.statemachine;

public interface AnimationState<T extends AnimationStateContext> {
    void update(T context);

    void entryAction(T context);

    void exitAction(T context);

    AnimationState<T> transition(T context, String condition);
}
