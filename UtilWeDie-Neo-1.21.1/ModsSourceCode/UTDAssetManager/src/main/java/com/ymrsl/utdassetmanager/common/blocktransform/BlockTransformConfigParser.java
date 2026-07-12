package com.ymrsl.utdassetmanager.common.blocktransform;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Activation;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.ActivationHand;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Catalyst;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.CatalystSource;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Creative;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Result;
import com.ymrsl.utdassetmanager.common.blocktransform.BlockTransformRule.Target;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Strict v1 parser. Any malformed rule invalidates the complete file. */
public final class BlockTransformConfigParser {
    private static final Pattern REGISTRY_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SIMPLE_RULE_ID = Pattern.compile("[a-z0-9_.-]{1,128}");
    private static final Pattern NAMESPACED_RULE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern PROPERTY_NAME = Pattern.compile("[a-z0-9_]+(?:[.-][a-z0-9_]+)*");

    private BlockTransformConfigParser() {
    }

    public static BlockTransformConfig parse(Reader reader) {
        JsonElement parsed = JsonParser.parseReader(reader);
        return parseRoot(requireObject(parsed, "$"));
    }

    public static BlockTransformConfig parse(String json) {
        JsonElement parsed = JsonParser.parseString(json);
        return parseRoot(requireObject(parsed, "$"));
    }

    private static BlockTransformConfig parseRoot(JsonObject root) {
        String schema = schema(root);
        if (!BlockTransformConfig.SCHEMA.equals(schema)) {
            throw invalid("$.schema", "expected " + BlockTransformConfig.SCHEMA);
        }
        JsonArray array = requireArray(required(root, "rules", "$"), "$.rules");
        List<BlockTransformRule> rules = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < array.size(); index++) {
            String path = "$.rules[" + index + "]";
            BlockTransformRule rule = parseRule(requireObject(array.get(index), path), path);
            if (!ids.add(rule.id())) {
                throw invalid(path + ".id", "duplicate rule id " + rule.id());
            }
            rules.add(rule);
        }
        rules.sort(Comparator.comparingInt(BlockTransformRule::priority).reversed()
                .thenComparing(BlockTransformRule::id));
        return new BlockTransformConfig(schema, rules);
    }

    private static BlockTransformRule parseRule(JsonObject object, String path) {
        String id = requiredString(object, "id", path, true).toLowerCase(Locale.ROOT);
        if (!SIMPLE_RULE_ID.matcher(id).matches() && !NAMESPACED_RULE_ID.matcher(id).matches()) {
            throw invalid(path + ".id", "must be a legacy simple id or a namespaced resource-location id");
        }
        boolean enabled = optionalBoolean(object, "enabled", path, false);
        int priority = optionalInt(object, "priority", path, 0);
        Target target = parseTarget(requireObject(required(object, "target", path), path + ".target"), path + ".target");
        Catalyst catalyst = parseCatalyst(
                requireObject(required(object, "catalyst", path), path + ".catalyst"), path + ".catalyst");
        Activation activation = object.has("activation")
                ? parseActivation(requireObject(object.get("activation"), path + ".activation"), path + ".activation")
                : new Activation(ActivationHand.MAIN, false, false);
        Result result = parseResult(requireObject(required(object, "result", path), path + ".result"), path + ".result");
        Creative creative = object.has("creative")
                ? parseCreative(requireObject(object.get("creative"), path + ".creative"), path + ".creative")
                : new Creative(true, false);
        if (creative.consume() && !creative.requireInput()) {
            throw invalid(path + ".creative", "consume=true requires requireInput=true");
        }
        return new BlockTransformRule(id, enabled, priority, target, catalyst, activation, result, creative);
    }

    private static Target parseTarget(JsonObject object, String path) {
        String block = registryId(requiredString(object, "block", path, true), path + ".block");
        Map<String, String> state = optionalState(object, "state", path);
        String policy = optionalString(object, "blockEntityPolicy", path, "reject", true).toLowerCase(Locale.ROOT);
        if (!"reject".equals(policy)) {
            throw invalid(path + ".blockEntityPolicy", "v1 only supports reject");
        }
        return new Target(block, state, policy);
    }

    private static Catalyst parseCatalyst(JsonObject object, String path) {
        String registryId = registryId(requiredString(object, "registryId", path, true), path + ".registryId");
        String discriminator = optionalString(object, "variantDiscriminator", path, "", false);
        String components = optionalString(object, "componentsSnbt", path, "{}", false).trim();
        if (components.isEmpty()) components = "{}";
        int count = optionalInt(object, "count", path, 1);
        if (count < 1) {
            throw invalid(path + ".count", "must be at least 1");
        }
        String sourceValue = optionalString(object, "source", path, "clicked_hand", true).toLowerCase(Locale.ROOT);
        CatalystSource source = switch (sourceValue) {
            case "clicked_hand" -> CatalystSource.CLICKED_HAND;
            case "inventory" -> CatalystSource.INVENTORY;
            default -> throw invalid(path + ".source", "expected clicked_hand or inventory");
        };
        boolean consume = optionalBoolean(object, "consume", path, true);
        return new Catalyst(registryId, discriminator, components, count, source, consume);
    }

    private static Activation parseActivation(JsonObject object, String path) {
        String handValue = optionalString(object, "hand", path, "main", true).toLowerCase(Locale.ROOT);
        ActivationHand hand = switch (handValue) {
            case "main" -> ActivationHand.MAIN;
            case "off" -> ActivationHand.OFF;
            case "any" -> ActivationHand.ANY;
            default -> throw invalid(path + ".hand", "expected main, off or any");
        };
        return new Activation(
                hand,
                optionalBoolean(object, "requireSneak", path, false),
                optionalBoolean(object, "allowFakePlayer", path, false));
    }

    private static Result parseResult(JsonObject object, String path) {
        String block = registryId(requiredString(object, "block", path, true), path + ".block");
        Map<String, String> state = optionalState(object, "state", path);
        List<String> copyProperties = new ArrayList<>();
        if (object.has("copyProperties")) {
            JsonArray values = requireArray(object.get("copyProperties"), path + ".copyProperties");
            Set<String> unique = new LinkedHashSet<>();
            for (int index = 0; index < values.size(); index++) {
                String property = primitiveString(values.get(index), path + ".copyProperties[" + index + "]", true)
                        .toLowerCase(Locale.ROOT);
                validatePropertyName(property, path + ".copyProperties[" + index + "]");
                if (!unique.add(property)) {
                    throw invalid(path + ".copyProperties[" + index + "]", "duplicate property " + property);
                }
            }
            copyProperties.addAll(unique);
        }
        return new Result(block, state, copyProperties);
    }

    private static Creative parseCreative(JsonObject object, String path) {
        return new Creative(
                optionalBoolean(object, "requireInput", path, true),
                optionalBoolean(object, "consume", path, false));
    }

    private static Map<String, String> optionalState(JsonObject object, String name, String path) {
        if (!object.has(name)) return Map.of();
        JsonObject state = requireObject(object.get(name), path + "." + name);
        Map<String, String> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
            String property = entry.getKey().trim().toLowerCase(Locale.ROOT);
            validatePropertyName(property, path + "." + name + "." + entry.getKey());
            String value = primitiveString(entry.getValue(), path + "." + name + "." + property, true)
                    .toLowerCase(Locale.ROOT);
            values.put(property, value);
        }
        return values;
    }

    private static void validatePropertyName(String property, String path) {
        if (!PROPERTY_NAME.matcher(property).matches()) {
            throw invalid(path, "invalid block-state property name");
        }
    }

    private static String registryId(String value, String path) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (!REGISTRY_ID.matcher(normalized).matches()) {
            throw invalid(path, "invalid namespaced registry id");
        }
        return normalized;
    }

    private static String schema(JsonObject root) {
        boolean hasPreferred = root.has("schema_version") && !root.get("schema_version").isJsonNull();
        boolean hasLegacy = root.has("schema") && !root.get("schema").isJsonNull();
        if (!hasPreferred && !hasLegacy) {
            throw invalid("$.schema_version", "is required (legacy $.schema is also accepted)");
        }
        String preferred = hasPreferred ? requiredString(root, "schema_version", "$", false) : "";
        String legacy = hasLegacy ? requiredString(root, "schema", "$", false) : "";
        if (hasPreferred && hasLegacy && !preferred.equals(legacy)) {
            throw invalid("$.schema_version", "conflicts with legacy $.schema");
        }
        return hasPreferred ? preferred : legacy;
    }

    private static JsonElement required(JsonObject object, String name, String path) {
        if (!object.has(name) || object.get(name).isJsonNull()) {
            throw invalid(path + "." + name, "is required");
        }
        return object.get(name);
    }

    private static String requiredString(JsonObject object, String name, String path, boolean nonBlank) {
        return primitiveString(required(object, name, path), path + "." + name, nonBlank);
    }

    private static String optionalString(JsonObject object, String name, String path, String defaultValue, boolean nonBlank) {
        if (!object.has(name) || object.get(name).isJsonNull()) return defaultValue;
        return primitiveString(object.get(name), path + "." + name, nonBlank);
    }

    private static String primitiveString(JsonElement element, String path, boolean nonBlank) {
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isString()) {
            throw invalid(path, "must be a string");
        }
        String value = primitive.getAsString().trim();
        if (nonBlank && value.isBlank()) {
            throw invalid(path, "must not be blank");
        }
        return value;
    }

    private static boolean optionalBoolean(JsonObject object, String name, String path, boolean defaultValue) {
        if (!object.has(name) || object.get(name).isJsonNull()) return defaultValue;
        JsonElement element = object.get(name);
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isBoolean()) {
            throw invalid(path + "." + name, "must be a boolean");
        }
        return primitive.getAsBoolean();
    }

    private static int optionalInt(JsonObject object, String name, String path, int defaultValue) {
        if (!object.has(name) || object.get(name).isJsonNull()) return defaultValue;
        JsonElement element = object.get(name);
        if (!(element instanceof JsonPrimitive primitive) || !primitive.isNumber()) {
            throw invalid(path + "." + name, "must be an integer");
        }
        try {
            int value = primitive.getAsInt();
            if (primitive.getAsBigDecimal().stripTrailingZeros().scale() > 0) {
                throw invalid(path + "." + name, "must be an integer");
            }
            return value;
        } catch (NumberFormatException | ArithmeticException error) {
            throw invalid(path + "." + name, "must be an integer");
        }
    }

    private static JsonObject requireObject(JsonElement element, String path) {
        if (element == null || !element.isJsonObject()) {
            throw invalid(path, "must be an object");
        }
        return element.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonElement element, String path) {
        if (element == null || !element.isJsonArray()) {
            throw invalid(path, "must be an array");
        }
        return element.getAsJsonArray();
    }

    private static IllegalArgumentException invalid(String path, String message) {
        return new IllegalArgumentException(path + ": " + message);
    }
}
