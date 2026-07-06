package io.github.ymrsl.firstpersonfoodeating.client.script.statemachine;

import io.github.ymrsl.firstpersonfoodeating.client.script.DiscreteTrackArray;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationController;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationRunner;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationRunner.PlayType;
import java.util.List;
import javax.annotation.Nullable;

public class AnimationStateContext {
    private boolean shouldHideCrossHair = false;
    private @Nullable AnimationStateMachine<?> stateMachine;
    private final DiscreteTrackArray trackArray = new DiscreteTrackArray();

    public @Nullable AnimationStateMachine<?> getStateMachine() {
        return stateMachine;
    }

    public DiscreteTrackArray getTrackArray() {
        return trackArray;
    }

    public int addTrackLine() {
        checkTrackArray();
        return trackArray.addTrackLine();
    }

    public void ensureTrackLineSize(int size) {
        checkTrackArray();
        trackArray.ensureCapacity(size);
    }

    public int getTrackLineSize() {
        checkTrackArray();
        return trackArray.getTrackLineSize();
    }

    public int assignNewTrack(int index) {
        checkTrackArray();
        return trackArray.assignNewTrack(index);
    }

    public int findIdleTrack(int index, boolean interruptHolding) {
        var stateMachine = checkStateMachine();
        checkTrackArray();
        List<Integer> trackList = trackArray.getByIndex(index);
        FoodAnimationController controller = stateMachine.getAnimationController();
        for (int track : trackList) {
            FoodAnimationRunner animation = controller.getAnimation(track);
            if (animation == null || animation.isStopped() || (interruptHolding && animation.isHolding())) {
                return track;
            }
        }
        return trackArray.assignNewTrack(index);
    }

    public void ensureTracksAmount(int index, int amount) {
        checkTrackArray();
        trackArray.ensureTrackAmount(index, amount);
    }

    public int getTrack(int trackLineIndex, int trackIndex) {
        checkTrackArray();
        if (trackLineIndex >= trackArray.getTrackLineSize()) {
            return -1;
        }
        List<Integer> tracks = trackArray.getByIndex(trackLineIndex);
        if (trackIndex >= tracks.size()) {
            return -1;
        }
        return tracks.get(trackIndex);
    }

    public int getAsSingletonTrack(int index) {
        checkTrackArray();
        List<Integer> trackList = trackArray.getByIndex(index);
        if (trackList.isEmpty()) {
            return trackArray.assignNewTrack(index);
        }
        return trackList.get(0);
    }

    public void runAnimation(String name, int track, boolean blending, int playType, float transitionTime) {
        var stateMachine = checkStateMachine();
        PlayType pt = decodePlayType(playType);
        stateMachine.getAnimationController().runAnimation(track, name, pt, transitionTime);
        stateMachine.getAnimationController().setBlending(track, blending);
    }

    public boolean hasAnimation(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        var stateMachine = checkStateMachine();
        return stateMachine.getAnimationController().containPrototype(name);
    }

    public void stopAnimation(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.stop();
        }
    }

    public void holdAnimation(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.hold();
        }
    }

    public void pauseAnimation(int track) {
        holdAnimation(track);
    }

    public void resumeAnimation(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner != null) {
            runner.run();
        }
    }

    public void setAnimationProgress(int track, float progress, boolean normalization) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner == null) {
            return;
        }
        if (normalization) {
            runner.setProgressNormalized(progress);
            return;
        }
        runner.setProgressNormalized(progress / Math.max(runner.getClip().lengthSeconds(), 0.0001f));
    }

    public void adjustAnimationProgress(int track, float progress, boolean normalization) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        if (runner == null) {
            return;
        }
        if (normalization) {
            runner.adjustProgress(runner.getClip().lengthSeconds() * progress);
            return;
        }
        runner.adjustProgress(progress);
    }

    public boolean isHolding(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        return runner != null && runner.isHolding();
    }

    public boolean isStopped(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        return runner == null || runner.isStopped();
    }

    public boolean isRunning(int track) {
        var stateMachine = checkStateMachine();
        FoodAnimationRunner runner = stateMachine.getAnimationController().getAnimation(track);
        return runner != null && runner.isRunning();
    }

    public void trigger(String input) {
        var stateMachine = checkStateMachine();
        stateMachine.trigger(input);
    }

    public boolean shouldHideCrossHair() {
        return shouldHideCrossHair;
    }

    public void setShouldHideCrossHair(boolean shouldHideCrossHair) {
        this.shouldHideCrossHair = shouldHideCrossHair;
    }

    void setStateMachine(@Nullable AnimationStateMachine<?> stateMachine) {
        if (this.stateMachine != null) {
            this.stateMachine.getAnimationController().setUpdatingTrackArray(null);
        }
        if (stateMachine != null) {
            stateMachine.getAnimationController().setUpdatingTrackArray(trackArray);
        }
        this.stateMachine = stateMachine;
    }

    private void checkTrackArray() {
        if (stateMachine != null && stateMachine.getAnimationController().getUpdatingTrackArray() != trackArray) {
            throw new TrackArrayMismatchException();
        }
    }

    private AnimationStateMachine<?> checkStateMachine() {
        if (stateMachine == null) {
            throw new IllegalStateException("Context is not bound to a state machine");
        }
        return stateMachine;
    }

    private static PlayType decodePlayType(int playType) {
        PlayType[] values = PlayType.values();
        if (playType < 0 || playType >= values.length) {
            return PlayType.PLAY_ONCE_HOLD;
        }
        return values[playType];
    }
}
