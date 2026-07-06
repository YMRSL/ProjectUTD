package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.Frame;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.LoopMode;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.BoneTrack;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.SoundEffectFrame;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAnimationClip.VectorTrack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector3f;

public final class BedrockAnimationBank {
    private static final String[] ADDITIVE_BONES = {
            "gun_and_rh",
            "ep_guan",
            "camera",
            "ep_camera",
            "aim_root",
            "ep_root",
            "gun_aim"
    };

    private final Map<String, FoodAnimationClip> clips = new HashMap<>();

    private BedrockAnimationBank() {
    }

    public static BedrockAnimationBank fromJson(JsonObject root) {
        BedrockAnimationBank bank = new BedrockAnimationBank();
        JsonObject animations = root.has("animations") && root.get("animations").isJsonObject()
                ? root.getAsJsonObject("animations")
                : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            FoodAnimationClip clip = parseClip(entry.getKey(), entry.getValue().getAsJsonObject());
            bank.registerClip(entry.getKey(), clip);
        }
        return bank;
    }

    public Map<String, FoodAnimationClip> clips() {
        return Map.copyOf(clips);
    }

    public FoodAnimationClip get(String key) {
        return clips.get(key);
    }

    private void registerClip(String rawName, FoodAnimationClip clip) {
        clips.put(rawName, clip);
        String alias = rawName;
        if (alias.startsWith("animation.")) {
            alias = alias.substring("animation.".length());
            clips.put(alias, clip);
        }
        int lastDot = alias.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < alias.length()) {
            clips.put(alias.substring(lastDot + 1), clip);
        }
    }

    private static FoodAnimationClip parseClip(String name, JsonObject clipObj) {
        LoopMode loopMode = parseLoopMode(clipObj.get("loop"));
        JsonObject bones = clipObj.has("bones") && clipObj.get("bones").isJsonObject()
                ? clipObj.getAsJsonObject("bones")
                : new JsonObject();
        JsonObject rootBone = bones.has("root") && bones.get("root").isJsonObject()
                ? bones.getAsJsonObject("root")
                : new JsonObject();

        VectorTrack rootPos = parseTrack(rootBone.get("position"));
        VectorTrack rootRot = parseTrack(rootBone.get("rotation"));
        List<VectorTrack> positionTracks = new ArrayList<>();
        List<VectorTrack> rotationTracks = new ArrayList<>();
        Map<String, BoneTrack> boneTracks = new HashMap<>();
        List<SoundEffectFrame> soundEffectFrames = parseSoundEffects(clipObj.get("sound_effects"));

        float inferredLength = Math.max(
                rootPos == null ? 0.0f : rootPos.maxTime(),
                rootRot == null ? 0.0f : rootRot.maxTime()
        );

        for (Map.Entry<String, JsonElement> entry : bones.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject bone = entry.getValue().getAsJsonObject();
            VectorTrack posTrack = parseTrack(bone.get("position"));
            VectorTrack rotTrack = parseTrack(bone.get("rotation"));
            VectorTrack scaleTrack = parseTrack(bone.get("scale"));
            if (posTrack == null && rotTrack == null && scaleTrack == null) {
                continue;
            }
            boneTracks.put(entry.getKey(), new BoneTrack(posTrack, rotTrack, scaleTrack));
            if (posTrack != null) {
                inferredLength = Math.max(inferredLength, posTrack.maxTime());
            }
            if (rotTrack != null) {
                inferredLength = Math.max(inferredLength, rotTrack.maxTime());
            }
            if (scaleTrack != null) {
                inferredLength = Math.max(inferredLength, scaleTrack.maxTime());
            }
        }

        float lengthSeconds = clipObj.has("animation_length") && clipObj.get("animation_length").isJsonPrimitive()
                ? clipObj.get("animation_length").getAsFloat()
                : inferredLength;
        if (lengthSeconds <= 0.0f) {
            lengthSeconds = 0.0001f;
        }

        if (rootPos == null) {
            rootPos = FoodAnimationClip.constant(new Vector3f());
        }
        if (rootRot == null) {
            rootRot = FoodAnimationClip.constant(new Vector3f());
        }
        positionTracks.add(rootPos);
        rotationTracks.add(rootRot);

        for (String boneName : ADDITIVE_BONES) {
            if (!bones.has(boneName) || !bones.get(boneName).isJsonObject()) {
                continue;
            }
            JsonObject bone = bones.getAsJsonObject(boneName);
            VectorTrack posTrack = parseTrack(bone.get("position"));
            VectorTrack rotTrack = parseTrack(bone.get("rotation"));
            if (posTrack != null && !posTrack.isAllZero(0.0001f)) {
                positionTracks.add(posTrack);
                inferredLength = Math.max(inferredLength, posTrack.maxTime());
            }
            if (rotTrack != null && !rotTrack.isAllZero(0.0001f)) {
                rotationTracks.add(rotTrack);
                inferredLength = Math.max(inferredLength, rotTrack.maxTime());
            }
        }

        if (lengthSeconds <= 0.0f) {
            lengthSeconds = Math.max(inferredLength, 0.0001f);
        }
        return new FoodAnimationClip(name, lengthSeconds, loopMode, positionTracks, rotationTracks, boneTracks, soundEffectFrames);
    }

    private static LoopMode parseLoopMode(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return LoopMode.PLAY_ONCE_STOP;
        }
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive().isBoolean()) {
                return element.getAsBoolean() ? LoopMode.LOOP : LoopMode.PLAY_ONCE_STOP;
            }
            if (element.getAsJsonPrimitive().isString()) {
                String v = element.getAsString();
                if ("true".equalsIgnoreCase(v) || "loop".equalsIgnoreCase(v)) {
                    return LoopMode.LOOP;
                }
                if ("hold_on_last_frame".equalsIgnoreCase(v)) {
                    return LoopMode.HOLD_ON_LAST_FRAME;
                }
            }
        }
        return LoopMode.PLAY_ONCE_STOP;
    }

    private static VectorTrack parseTrack(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        List<Frame> frames = new ArrayList<>();
        if (element.isJsonArray()) {
            frames.add(new Frame(0.0f, readVector(element.getAsJsonArray())));
            return new VectorTrack(frames);
        }
        if (element.isJsonPrimitive()) {
            if (!element.getAsJsonPrimitive().isNumber()) {
                frames.add(new Frame(0.0f, new Vector3f()));
                return new VectorTrack(frames);
            }
            float v = element.getAsFloat();
            frames.add(new Frame(0.0f, new Vector3f(v, v, v)));
            return new VectorTrack(frames);
        }
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject obj = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> frameEntry : obj.entrySet()) {
            float t;
            try {
                t = Float.parseFloat(frameEntry.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            JsonElement frameValue = frameEntry.getValue();
            if (frameValue != null && frameValue.isJsonObject()) {
                JsonObject frameObj = frameValue.getAsJsonObject();
                boolean hasPre = frameObj.has("pre") && frameObj.get("pre").isJsonArray();
                boolean hasPost = frameObj.has("post") && frameObj.get("post").isJsonArray();
                if (hasPre && hasPost) {
                    // Preserve Bedrock pre/post step at the same key time.
                    frames.add(new Frame(t, readVector(frameObj.getAsJsonArray("pre"))));
                    frames.add(new Frame(t + 0.0001f, readVector(frameObj.getAsJsonArray("post"))));
                    continue;
                }
            }
            Vector3f vec = readFrameValue(frameValue);
            frames.add(new Frame(t, vec));
        }
        if (frames.isEmpty()) {
            return null;
        }
        return new VectorTrack(frames);
    }

    private static List<SoundEffectFrame> parseSoundEffects(JsonElement element) {
        List<SoundEffectFrame> frames = new ArrayList<>();
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return frames;
        }
        JsonObject obj = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            float time;
            try {
                time = Float.parseFloat(entry.getKey());
            } catch (NumberFormatException ignored) {
                continue;
            }
            collectSoundEffectsAtTime(time, entry.getValue(), frames);
        }
        return frames;
    }

    private static void collectSoundEffectsAtTime(float time, JsonElement element, List<SoundEffectFrame> out) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collectSoundEffectsAtTime(time, child, out);
            }
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("effect") && obj.get("effect").isJsonPrimitive()) {
            String effect = obj.get("effect").getAsString();
            if (effect != null && !effect.isBlank()) {
                out.add(new SoundEffectFrame(time, effect));
            }
        }
    }

    private static Vector3f readFrameValue(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return new Vector3f();
        }
        if (element.isJsonArray()) {
            return readVector(element.getAsJsonArray());
        }
        if (element.isJsonPrimitive()) {
            if (!element.getAsJsonPrimitive().isNumber()) {
                return new Vector3f();
            }
            float v = element.getAsFloat();
            return new Vector3f(v, v, v);
        }
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            if (obj.has("post") && obj.get("post").isJsonArray()) {
                return readVector(obj.getAsJsonArray("post"));
            }
            if (obj.has("pre") && obj.get("pre").isJsonArray()) {
                return readVector(obj.getAsJsonArray("pre"));
            }
        }
        return new Vector3f();
    }

    private static Vector3f readVector(JsonArray arr) {
        float x = arr.size() > 0 && arr.get(0).isJsonPrimitive() && arr.get(0).getAsJsonPrimitive().isNumber()
                ? arr.get(0).getAsFloat() : 0.0f;
        float y = arr.size() > 1 && arr.get(1).isJsonPrimitive() && arr.get(1).getAsJsonPrimitive().isNumber()
                ? arr.get(1).getAsFloat() : 0.0f;
        float z = arr.size() > 2 && arr.get(2).isJsonPrimitive() && arr.get(2).getAsJsonPrimitive().isNumber()
                ? arr.get(2).getAsFloat() : 0.0f;
        return new Vector3f(x, y, z);
    }
}
