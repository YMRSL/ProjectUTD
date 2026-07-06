package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public final class FoodAnimationClip {
    public enum LoopMode {
        LOOP,
        HOLD_ON_LAST_FRAME,
        PLAY_ONCE_STOP
    }

    public record Frame(float time, Vector3f value) {
    }

    public record SoundEffectFrame(float time, String effect) {
    }

    public static final class VectorTrack {
        private final List<Frame> frames;

        public VectorTrack(List<Frame> frames) {
            this.frames = new ArrayList<>(frames);
            this.frames.sort(Comparator.comparing(Frame::time));
        }

        public Vector3f sample(float time) {
            if (frames.isEmpty()) {
                return new Vector3f();
            }
            if (frames.size() == 1 || time <= frames.get(0).time()) {
                return new Vector3f(frames.get(0).value());
            }
            int lastIndex = frames.size() - 1;
            if (time >= frames.get(lastIndex).time()) {
                return new Vector3f(frames.get(lastIndex).value());
            }
            Frame a = frames.get(0);
            for (int i = 1; i < frames.size(); i++) {
                Frame b = frames.get(i);
                if (time <= b.time()) {
                    float alpha = Mth.clamp((time - a.time()) / Math.max(0.0001f, b.time() - a.time()), 0.0f, 1.0f);
                    return new Vector3f(
                            Mth.lerp(alpha, a.value().x(), b.value().x()),
                            Mth.lerp(alpha, a.value().y(), b.value().y()),
                            Mth.lerp(alpha, a.value().z(), b.value().z())
                    );
                }
                a = b;
            }
            return new Vector3f(frames.get(lastIndex).value());
        }

        public float maxTime() {
            if (frames.isEmpty()) {
                return 0.0f;
            }
            return frames.get(frames.size() - 1).time();
        }

        public boolean isAllZero(float epsilon) {
            for (Frame frame : frames) {
                if (Math.abs(frame.value().x()) > epsilon
                        || Math.abs(frame.value().y()) > epsilon
                        || Math.abs(frame.value().z()) > epsilon) {
                    return false;
                }
            }
            return true;
        }
    }

    public static final class BoneTrack {
        private final VectorTrack positionTrack;
        private final VectorTrack rotationTrack;
        private final VectorTrack scaleTrack;

        public BoneTrack(VectorTrack positionTrack, VectorTrack rotationTrack, VectorTrack scaleTrack) {
            this.positionTrack = positionTrack;
            this.rotationTrack = rotationTrack;
            this.scaleTrack = scaleTrack;
        }

        public BoneSample sample(float timeSeconds) {
            boolean hasPosition = positionTrack != null;
            boolean hasRotation = rotationTrack != null;
            boolean hasScale = scaleTrack != null;
            Vector3f position = hasPosition ? positionTrack.sample(timeSeconds) : new Vector3f();
            Vector3f rotationDeg = hasRotation ? rotationTrack.sample(timeSeconds) : new Vector3f();
            Vector3f scale = hasScale ? scaleTrack.sample(timeSeconds) : new Vector3f(1.0f, 1.0f, 1.0f);
            return new BoneSample(hasPosition, position, hasRotation, rotationDeg, hasScale, scale);
        }

        public float maxTime() {
            float max = 0.0f;
            if (positionTrack != null) {
                max = Math.max(max, positionTrack.maxTime());
            }
            if (rotationTrack != null) {
                max = Math.max(max, rotationTrack.maxTime());
            }
            if (scaleTrack != null) {
                max = Math.max(max, scaleTrack.maxTime());
            }
            return max;
        }
    }

    public record BoneSample(
            boolean hasPosition,
            Vector3f position,
            boolean hasRotation,
            Vector3f rotationDeg,
            boolean hasScale,
            Vector3f scale
    ) {
    }

    public record RootTransform(Vector3f position, Vector3f rotationDeg) {
        public static final RootTransform IDENTITY = new RootTransform(new Vector3f(), new Vector3f());

        public RootTransform add(RootTransform other) {
            return new RootTransform(new Vector3f(position).add(other.position), new Vector3f(rotationDeg).add(other.rotationDeg));
        }
    }

    private final String name;
    private final float lengthSeconds;
    private final LoopMode loopMode;
    private final List<VectorTrack> positionTracks;
    private final List<VectorTrack> rotationTracks;
    private final Map<String, BoneTrack> boneTracks;
    private final List<SoundEffectFrame> soundEffectFrames;

    public FoodAnimationClip(
            String name,
            float lengthSeconds,
            LoopMode loopMode,
            VectorTrack rootPositionTrack,
            VectorTrack rootRotationTrack
    ) {
        this(name, lengthSeconds, loopMode, List.of(rootPositionTrack), List.of(rootRotationTrack), Map.of(), List.of());
    }

    public FoodAnimationClip(
            String name,
            float lengthSeconds,
            LoopMode loopMode,
            List<VectorTrack> positionTracks,
            List<VectorTrack> rotationTracks,
            Map<String, BoneTrack> boneTracks,
            List<SoundEffectFrame> soundEffectFrames
    ) {
        this.name = name;
        this.lengthSeconds = Math.max(lengthSeconds, 0.0001f);
        this.loopMode = loopMode;
        this.positionTracks = new ArrayList<>(positionTracks);
        this.rotationTracks = new ArrayList<>(rotationTracks);
        this.boneTracks = new HashMap<>(boneTracks);
        this.soundEffectFrames = new ArrayList<>(soundEffectFrames);
        this.soundEffectFrames.sort(Comparator.comparing(SoundEffectFrame::time));
    }

    public FoodAnimationClip(
            String name,
            float lengthSeconds,
            LoopMode loopMode,
            List<VectorTrack> positionTracks,
            List<VectorTrack> rotationTracks
    ) {
        this(name, lengthSeconds, loopMode, positionTracks, rotationTracks, Map.of(), List.of());
    }

    public FoodAnimationClip(
            String name,
            float lengthSeconds,
            LoopMode loopMode,
            List<VectorTrack> positionTracks,
            List<VectorTrack> rotationTracks,
            Map<String, BoneTrack> boneTracks
    ) {
        this(name, lengthSeconds, loopMode, positionTracks, rotationTracks, boneTracks, List.of());
    }

    public String name() {
        return name;
    }

    public float lengthSeconds() {
        return lengthSeconds;
    }

    public LoopMode loopMode() {
        return loopMode;
    }

    public RootTransform sampleRoot(float timeSeconds) {
        Vector3f pos = new Vector3f();
        for (VectorTrack track : positionTracks) {
            if (track != null) {
                pos.add(track.sample(timeSeconds));
            }
        }
        Vector3f rot = new Vector3f();
        for (VectorTrack track : rotationTracks) {
            if (track != null) {
                rot.add(track.sample(timeSeconds));
            }
        }
        return new RootTransform(pos, rot);
    }

    public Map<String, BoneSample> sampleBones(float timeSeconds) {
        Map<String, BoneSample> sampled = new HashMap<>();
        for (Map.Entry<String, BoneTrack> entry : boneTracks.entrySet()) {
            BoneTrack track = entry.getValue();
            if (track == null) {
                continue;
            }
            BoneSample sample = track.sample(timeSeconds);
            if (sample.hasPosition() || sample.hasRotation() || sample.hasScale()) {
                sampled.put(entry.getKey(), sample);
            }
        }
        return sampled;
    }

    public List<SoundEffectFrame> soundEffectFrames() {
        return List.copyOf(soundEffectFrames);
    }

    public static VectorTrack constant(Vector3f value) {
        return new VectorTrack(List.of(new Frame(0.0f, new Vector3f(value))));
    }
}
