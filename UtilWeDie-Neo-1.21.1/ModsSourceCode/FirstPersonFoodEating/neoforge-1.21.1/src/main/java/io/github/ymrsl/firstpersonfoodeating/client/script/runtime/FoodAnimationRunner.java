package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.RootTransform;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.BoneSample;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FoodAnimationRunner {
    public enum PlayType {
        PLAY_ONCE_HOLD,
        PLAY_ONCE_STOP,
        LOOP
    }

    private final FoodAnimationClip clip;
    private final PlayType playType;
    private float progressSec = 0.0f;
    private boolean running = true;
    private boolean holding = false;
    private boolean stopped = false;
    private final ArrayList<String> triggeredSoundEffects = new ArrayList<>();

    public FoodAnimationRunner(FoodAnimationClip clip, PlayType playType) {
        this.clip = clip;
        this.playType = playType;
        enqueueSoundEffectsAtStart();
    }

    public FoodAnimationClip getClip() {
        return clip;
    }

    public float getProgressSeconds() {
        return progressSec;
    }

    public float getLengthSeconds() {
        return clip.lengthSeconds();
    }

    public void update(float deltaSec) {
        if (!running) {
            return;
        }
        float previous = progressSec;
        progressSec += Math.max(deltaSec, 0.0f);
        float length = Math.max(clip.lengthSeconds(), 0.0001f);
        if (playType == PlayType.LOOP) {
            queueLoopSoundEffects(previous, progressSec, length);
            progressSec %= length;
            return;
        }
        float clampedProgress = Math.min(progressSec, length);
        queueLinearSoundEffects(previous, clampedProgress);
        if (progressSec < length) {
            return;
        }
        if (playType == PlayType.PLAY_ONCE_HOLD) {
            progressSec = length;
            running = false;
            holding = true;
            stopped = false;
            return;
        }
        progressSec = length;
        running = false;
        holding = false;
        stopped = true;
    }

    public RootTransform sample() {
        return sample(0.0f);
    }

    public RootTransform sample(float framePartialTicks) {
        return clip.sampleRoot(sampleTime(framePartialTicks));
    }

    public Map<String, BoneSample> sampleBones() {
        return sampleBones(0.0f);
    }

    public Map<String, BoneSample> sampleBones(float framePartialTicks) {
        return clip.sampleBones(sampleTime(framePartialTicks));
    }

    public void setProgressNormalized(float normalized) {
        progressSec = clip.lengthSeconds() * normalized;
    }

    public void adjustProgress(float deltaSec) {
        progressSec += deltaSec;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isHolding() {
        return holding;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void stop() {
        running = false;
        holding = false;
        stopped = true;
    }

    public void hold() {
        progressSec = clip.lengthSeconds();
        running = false;
        holding = true;
        stopped = false;
    }

    public void run() {
        running = true;
        if (!stopped && !holding) {
            return;
        }
        if (progressSec >= clip.lengthSeconds()) {
            progressSec = 0.0f;
            enqueueSoundEffectsAtStart();
        }
        holding = false;
        stopped = false;
    }

    public List<String> drainTriggeredSoundEffects() {
        if (triggeredSoundEffects.isEmpty()) {
            return List.of();
        }
        List<String> drained = List.copyOf(triggeredSoundEffects);
        triggeredSoundEffects.clear();
        return drained;
    }

    private float sampleTime(float framePartialTicks) {
        float time = progressSec;
        if (running && framePartialTicks > 0.0f) {
            time += framePartialTicks / 20.0f;
        }
        float length = Math.max(clip.lengthSeconds(), 0.0001f);
        if (playType == PlayType.LOOP) {
            time %= length;
            if (time < 0.0f) {
                time += length;
            }
            return time;
        }
        return Math.min(time, length);
    }

    private void enqueueSoundEffectsAtStart() {
        for (FoodAnimationClip.SoundEffectFrame frame : clip.soundEffectFrames()) {
            if (frame.time() <= 0.0001f) {
                triggeredSoundEffects.add(frame.effect());
            }
        }
    }

    private void queueLinearSoundEffects(float fromSec, float toSec) {
        if (toSec <= fromSec) {
            return;
        }
        for (FoodAnimationClip.SoundEffectFrame frame : clip.soundEffectFrames()) {
            float t = frame.time();
            if (t > fromSec + 0.0001f && t <= toSec + 0.0001f) {
                triggeredSoundEffects.add(frame.effect());
            }
        }
    }

    private void queueLoopSoundEffects(float fromSec, float toSec, float length) {
        if (toSec <= fromSec || length <= 0.0f) {
            return;
        }
        int startCycle = (int) Math.floor(fromSec / length);
        int endCycle = (int) Math.floor(toSec / length);
        float cycleStart = positiveModulo(fromSec, length);
        float cycleEnd = positiveModulo(toSec, length);

        if (startCycle == endCycle) {
            queueLoopSegment(cycleStart, cycleEnd, false);
            return;
        }

        queueLoopSegment(cycleStart, length, false);
        int middleCycles = Math.min(Math.max(endCycle - startCycle - 1, 0), 8);
        for (int i = 0; i < middleCycles; i++) {
            queueLoopSegment(0.0f, length, true);
        }
        queueLoopSegment(0.0f, cycleEnd, true);
    }

    private void queueLoopSegment(float fromSec, float toSec, boolean includeStart) {
        if (toSec < fromSec) {
            return;
        }
        for (FoodAnimationClip.SoundEffectFrame frame : clip.soundEffectFrames()) {
            float t = frame.time();
            boolean passStart = includeStart ? t >= fromSec - 0.0001f : t > fromSec + 0.0001f;
            if (passStart && t <= toSec + 0.0001f) {
                triggeredSoundEffects.add(frame.effect());
            }
        }
    }

    private static float positiveModulo(float value, float divisor) {
        float mod = value % divisor;
        if (mod < 0.0f) {
            mod += divisor;
        }
        return mod;
    }
}
