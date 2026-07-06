package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.RootTransform;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.BoneSample;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationRunner.PlayType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3f;

public final class FoodAnimationController {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Map<String, FoodAnimationClip> prototypes = new HashMap<>();
    private final ArrayList<FoodAnimationRunner> currentRunners = new ArrayList<>();
    private final ArrayList<TrackTransition> trackTransitions = new ArrayList<>();
    private final ArrayList<Boolean> blending = new ArrayList<>();
    private @Nullable Iterable<Integer> updatingTrackArray = null;

    public record BonePose(Vector3f position, Vector3f rotationDeg, Vector3f scale) {
        public static final BonePose IDENTITY = new BonePose(
                new Vector3f(),
                new Vector3f(),
                new Vector3f(1.0f, 1.0f, 1.0f)
        );
    }

    public FoodAnimationController(Map<String, FoodAnimationClip> prototypes) {
        this.prototypes.putAll(prototypes);
    }

    public boolean containPrototype(String name) {
        return prototypes.containsKey(name);
    }

    public void providePrototypeIfAbsent(String name, FoodAnimationClip clip) {
        prototypes.putIfAbsent(name, clip);
    }

    public @Nullable FoodAnimationRunner getAnimation(int track) {
        if (track >= currentRunners.size()) {
            return null;
        }
        return currentRunners.get(track);
    }

    public void runAnimation(int track, String animationName, PlayType playType, float transitionTimeS) {
        FoodAnimationClip clip = prototypes.get(animationName);
        if (clip == null) {
            LOGGER.warn("[firstpersonfoodeating] Missing animation clip '{}' (track={})", animationName, track);
            return;
        }
        ensureTrackSize(track);

        FoodAnimationRunner previous = currentRunners.get(track);
        FoodAnimationRunner next = new FoodAnimationRunner(clip, playType);
        currentRunners.set(track, next);

        float durationSec = Math.max(transitionTimeS, 0.0f);
        if (previous != null && !previous.isStopped() && durationSec > 0.0001f) {
            trackTransitions.set(track, new TrackTransition(previous, durationSec));
        } else {
            trackTransitions.set(track, null);
        }
    }

    public void removeAnimation(int track) {
        if (track < currentRunners.size()) {
            currentRunners.set(track, null);
        }
        if (track < trackTransitions.size()) {
            trackTransitions.set(track, null);
        }
    }

    public void setBlending(int track, boolean blend) {
        for (int i = blending.size(); i <= track; i++) {
            blending.add(false);
        }
        blending.set(track, blend);
    }

    public @Nullable Iterable<Integer> getUpdatingTrackArray() {
        return updatingTrackArray;
    }

    public void setUpdatingTrackArray(@Nullable Iterable<Integer> updatingTrackArray) {
        this.updatingTrackArray = updatingTrackArray;
    }

    public void update(float deltaSec) {
        if (updatingTrackArray != null) {
            for (int track : updatingTrackArray) {
                updateTrack(track, deltaSec);
            }
            return;
        }
        for (int i = 0; i < currentRunners.size(); i++) {
            updateTrack(i, deltaSec);
        }
    }

    private void updateTrack(int track, float deltaSec) {
        if (track >= currentRunners.size() && track >= trackTransitions.size()) {
            return;
        }
        FoodAnimationRunner runner = track < currentRunners.size() ? currentRunners.get(track) : null;
        if (runner == null) {
            TrackTransition transition = track < trackTransitions.size() ? trackTransitions.get(track) : null;
            if (transition != null) {
                transition.update(deltaSec);
                if (transition.isFinished()) {
                    trackTransitions.set(track, null);
                }
            }
            return;
        }
        runner.update(deltaSec);

        if (track >= trackTransitions.size()) {
            return;
        }
        TrackTransition transition = trackTransitions.get(track);
        if (transition == null) {
            return;
        }
        transition.update(deltaSec);
        if (transition.isFinished()) {
            trackTransitions.set(track, null);
        }
    }

    public RootTransform sampleCombinedRootTransform() {
        return sampleCombinedRootTransform(0.0f);
    }

    public RootTransform sampleCombinedRootTransform(float framePartialTicks) {
        RootTransform result = RootTransform.IDENTITY;
        List<Integer> tracks = new ArrayList<>(currentRunners.size());
        for (int i = 0; i < currentRunners.size(); i++) {
            tracks.add(i);
        }
        for (int track : tracks) {
            RootTransform sampled = sampleTrackRoot(track, framePartialTicks);
            if (sampled == null) {
                continue;
            }
            boolean blend = track < blending.size() && blending.get(track);
            if (blend) {
                result = result.add(sampled);
            } else {
                result = sampled;
            }
        }
        return result;
    }

    public Map<String, BonePose> sampleCombinedBonePose() {
        return sampleCombinedBonePose(0.0f);
    }

    public Map<String, BonePose> sampleCombinedBonePose(float framePartialTicks) {
        Map<String, MutableBonePose> accum = new HashMap<>();
        List<Integer> tracks = new ArrayList<>(currentRunners.size());
        for (int i = 0; i < currentRunners.size(); i++) {
            tracks.add(i);
        }
        for (int track : tracks) {
            Map<String, BoneSample> sampled = sampleTrackBones(track, framePartialTicks);
            if (sampled.isEmpty()) {
                continue;
            }
            boolean blend = track < blending.size() && blending.get(track);
            for (Map.Entry<String, BoneSample> entry : sampled.entrySet()) {
                String boneName = entry.getKey();
                BoneSample boneSample = entry.getValue();
                MutableBonePose mutable = accum.computeIfAbsent(boneName, k -> new MutableBonePose());
                if (boneSample.hasPosition()) {
                    if (blend) {
                        mutable.position.add(boneSample.position());
                    } else {
                        mutable.position.set(boneSample.position());
                    }
                }
                if (boneSample.hasRotation()) {
                    if (blend) {
                        mutable.rotationDeg.add(boneSample.rotationDeg());
                    } else {
                        mutable.rotationDeg.set(boneSample.rotationDeg());
                    }
                }
                if (boneSample.hasScale()) {
                    if (blend) {
                        mutable.scale.mul(boneSample.scale());
                    } else {
                        mutable.scale.set(boneSample.scale());
                    }
                }
            }
        }

        Map<String, BonePose> result = new HashMap<>();
        for (Map.Entry<String, MutableBonePose> entry : accum.entrySet()) {
            MutableBonePose m = entry.getValue();
            result.put(entry.getKey(), new BonePose(
                    new Vector3f(m.position),
                    new Vector3f(m.rotationDeg),
                    new Vector3f(m.scale)
            ));
        }
        return result;
    }

    public List<String> drainTriggeredSoundEffects() {
        if (currentRunners.isEmpty()) {
            return List.of();
        }
        List<String> drained = new ArrayList<>();
        for (FoodAnimationRunner runner : currentRunners) {
            if (runner == null) {
                continue;
            }
            drained.addAll(runner.drainTriggeredSoundEffects());
        }
        return drained;
    }

    private void ensureTrackSize(int track) {
        for (int i = currentRunners.size(); i <= track; i++) {
            currentRunners.add(null);
            trackTransitions.add(null);
        }
    }

    private @Nullable RootTransform sampleTrackRoot(int track, float framePartialTicks) {
        FoodAnimationRunner runner = track < currentRunners.size() ? currentRunners.get(track) : null;
        TrackTransition transition = track < trackTransitions.size() ? trackTransitions.get(track) : null;

        if (transition != null) {
            RootTransform from = transition.fromRunner().sample(framePartialTicks);
            RootTransform to = runner == null || runner.isStopped()
                    ? RootTransform.IDENTITY
                    : runner.sample(framePartialTicks);
            return lerpRoot(from, to, transition.alpha());
        }

        if (runner == null || runner.isStopped()) {
            return null;
        }
        return runner.sample(framePartialTicks);
    }

    private Map<String, BoneSample> sampleTrackBones(int track, float framePartialTicks) {
        FoodAnimationRunner runner = track < currentRunners.size() ? currentRunners.get(track) : null;
        TrackTransition transition = track < trackTransitions.size() ? trackTransitions.get(track) : null;

        if (transition != null) {
            Map<String, BoneSample> from = transition.fromRunner().sampleBones(framePartialTicks);
            Map<String, BoneSample> to = runner == null || runner.isStopped()
                    ? Map.of()
                    : runner.sampleBones(framePartialTicks);
            return blendBoneSamples(from, to, transition.alpha());
        }

        if (runner == null || runner.isStopped()) {
            return Map.of();
        }
        return runner.sampleBones(framePartialTicks);
    }

    private static RootTransform lerpRoot(RootTransform from, RootTransform to, float alpha) {
        float t = clamp01(alpha);
        return new RootTransform(
                lerpVec(from.position(), to.position(), t),
                lerpVec(from.rotationDeg(), to.rotationDeg(), t)
        );
    }

    private static Map<String, BoneSample> blendBoneSamples(
            Map<String, BoneSample> from,
            Map<String, BoneSample> to,
            float alpha
    ) {
        float t = clamp01(alpha);
        Set<String> keys = new HashSet<>();
        keys.addAll(from.keySet());
        keys.addAll(to.keySet());

        Map<String, BoneSample> blended = new HashMap<>();
        for (String key : keys) {
            BoneSample a = from.get(key);
            BoneSample b = to.get(key);

            boolean hasPosA = a != null && a.hasPosition();
            boolean hasPosB = b != null && b.hasPosition();
            boolean hasRotA = a != null && a.hasRotation();
            boolean hasRotB = b != null && b.hasRotation();
            boolean hasScaleA = a != null && a.hasScale();
            boolean hasScaleB = b != null && b.hasScale();

            boolean hasPos = hasPosA || hasPosB;
            boolean hasRot = hasRotA || hasRotB;
            boolean hasScale = hasScaleA || hasScaleB;

            if (!hasPos && !hasRot && !hasScale) {
                continue;
            }

            Vector3f posFrom = hasPosA ? a.position() : new Vector3f();
            Vector3f posTo = hasPosB ? b.position() : new Vector3f();
            Vector3f rotFrom = hasRotA ? a.rotationDeg() : new Vector3f();
            Vector3f rotTo = hasRotB ? b.rotationDeg() : new Vector3f();
            Vector3f scaleFrom = hasScaleA ? a.scale() : new Vector3f(1.0f, 1.0f, 1.0f);
            Vector3f scaleTo = hasScaleB ? b.scale() : new Vector3f(1.0f, 1.0f, 1.0f);

            blended.put(key, new BoneSample(
                    hasPos,
                    lerpVec(posFrom, posTo, t),
                    hasRot,
                    lerpVec(rotFrom, rotTo, t),
                    hasScale,
                    lerpVec(scaleFrom, scaleTo, t)
            ));
        }
        return blended;
    }

    private static Vector3f lerpVec(Vector3f from, Vector3f to, float alpha) {
        return new Vector3f(from).lerp(to, clamp01(alpha));
    }

    private static float clamp01(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        if (value > 1.0f) {
            return 1.0f;
        }
        return value;
    }

    private static final class MutableBonePose {
        private final Vector3f position = new Vector3f();
        private final Vector3f rotationDeg = new Vector3f();
        private final Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
    }

    private static final class TrackTransition {
        private final FoodAnimationRunner fromRunner;
        private final float durationSec;
        private float elapsedSec;

        private TrackTransition(FoodAnimationRunner fromRunner, float durationSec) {
            this.fromRunner = fromRunner;
            this.durationSec = Math.max(durationSec, 0.0001f);
            this.elapsedSec = 0.0f;
        }

        private FoodAnimationRunner fromRunner() {
            return fromRunner;
        }

        private void update(float deltaSec) {
            fromRunner.update(deltaSec);
            elapsedSec += Math.max(deltaSec, 0.0f);
        }

        private float alpha() {
            return clamp01(elapsedSec / durationSec);
        }

        private boolean isFinished() {
            return elapsedSec >= durationSec;
        }
    }
}
