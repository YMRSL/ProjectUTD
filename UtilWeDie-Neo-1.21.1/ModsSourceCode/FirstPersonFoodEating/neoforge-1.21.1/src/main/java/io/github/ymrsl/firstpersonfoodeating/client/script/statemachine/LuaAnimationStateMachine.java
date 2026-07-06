package io.github.ymrsl.firstpersonfoodeating.client.script.statemachine;

import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import java.util.function.Consumer;

public class LuaAnimationStateMachine<T extends AnimationStateContext> extends AnimationStateMachine<T> {
    Consumer<T> initializeFunc;
    Consumer<T> exitFunc;

    LuaAnimationStateMachine(FoodAnimationController animationController) {
        super(animationController);
    }

    @Override
    public void initialize() {
        if (initializeFunc != null) {
            initializeFunc.accept(this.context);
        }
        super.initialize();
    }

    @Override
    public void exit() {
        if (exitFunc != null) {
            exitFunc.accept(this.context);
        }
        super.exit();
    }
}
