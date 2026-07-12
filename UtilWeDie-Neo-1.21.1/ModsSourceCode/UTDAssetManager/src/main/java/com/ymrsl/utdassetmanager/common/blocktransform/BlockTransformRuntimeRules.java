package com.ymrsl.utdassetmanager.common.blocktransform;

import com.mojang.logging.LogUtils;
import com.ymrsl.utdassetmanager.core.AssetIdentity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.slf4j.Logger;

/** Resolves the pure JSON model against the live Minecraft registries, fail-closed per file generation. */
final class BlockTransformRuntimeRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static long compiledGeneration = Long.MIN_VALUE;
    private static CompiledSet compiled = CompiledSet.empty("not compiled");

    private BlockTransformRuntimeRules() {
    }

    static synchronized CompiledSet current() {
        BlockTransformRepository.Snapshot source = BlockTransformRepository.get().snapshot();
        if (source.generation() == compiledGeneration) return compiled;
        compiledGeneration = source.generation();
        if (!source.usable()) {
            compiled = CompiledSet.empty(source.error());
            return compiled;
        }
        try {
            List<CompiledRule> rules = new ArrayList<>();
            for (BlockTransformRule rule : source.rules()) {
                if (rule.enabled()) rules.add(compile(rule));
            }
            compiled = new CompiledSet(source.generation(), rules, "");
            LOGGER.info("Enabled {} UTD block transform rule(s)", rules.size());
        } catch (RuntimeException invalid) {
            compiled = new CompiledSet(source.generation(), List.of(),
                    "runtime validation failed; all transforms disabled: " + invalid.getMessage());
            LOGGER.error("UTD block transforms disabled: {}", compiled.error());
        }
        return compiled;
    }

    private static CompiledRule compile(BlockTransformRule rule) {
        Block target = block(rule.target().block(), rule.id() + ".target.block");
        Block result = block(rule.result().block(), rule.id() + ".result.block");
        Item catalyst = item(rule.catalyst().registryId(), rule.id() + ".catalyst.registryId");
        if (hasAnyBlockEntity(target)) {
            throw invalid(rule, "target block can create a BlockEntity; v1 policy is reject");
        }
        if (hasAnyBlockEntity(result)) {
            throw invalid(rule, "result block can create a BlockEntity; v1 policy is reject");
        }
        if (isMultiBlockSensitive(target)) {
            throw invalid(rule, "target block is multi-block sensitive and is rejected by v1");
        }
        if (isMultiBlockSensitive(result)) {
            throw invalid(rule, "result block is multi-block sensitive and is rejected by v1");
        }

        validateStateMap(target.defaultBlockState(), rule.target().state(), rule, "target.state");
        validateStateMap(result.defaultBlockState(), rule.result().state(), rule, "result.state");
        for (String name : rule.result().copyProperties()) {
            Property<?> source = target.getStateDefinition().getProperty(name);
            Property<?> destination = result.getStateDefinition().getProperty(name);
            if (source == null || destination == null) {
                throw invalid(rule, "copyProperties property '" + name + "' must exist on both blocks");
            }
            for (Comparable<?> value : source.getPossibleValues()) {
                String serialized = propertyNameUnchecked(source, value);
                if (destination.getValue(serialized).isEmpty()) {
                    throw invalid(rule, "copyProperties property '" + name + "' has incompatible values");
                }
            }
        }

        CompoundTag expectedComponents = null;
        String components = rule.catalyst().componentsSnbt().trim();
        if (!components.isBlank() && !"{}".equals(components)) {
            try {
                expectedComponents = TagParser.parseTag(components);
            } catch (Exception malformed) {
                throw invalid(rule, "catalyst.componentsSnbt is invalid SNBT: " + malformed.getMessage());
            }
        }
        return new CompiledRule(rule, target, result, catalyst, expectedComponents);
    }

    private static Block block(String id, String path) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.BLOCK.containsKey(key)) {
            throw new IllegalArgumentException(path + " references missing block " + id);
        }
        return BuiltInRegistries.BLOCK.get(key);
    }

    private static Item item(String id, String path) {
        ResourceLocation key = ResourceLocation.parse(id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) {
            throw new IllegalArgumentException(path + " references missing item " + id);
        }
        return BuiltInRegistries.ITEM.get(key);
    }

    private static boolean hasAnyBlockEntity(Block block) {
        return block.getStateDefinition().getPossibleStates().stream().anyMatch(BlockState::hasBlockEntity);
    }

    private static boolean isMultiBlockSensitive(Block block) {
        if (block instanceof BedBlock || block instanceof DoorBlock || block instanceof DoublePlantBlock
                || block instanceof PistonHeadBlock) {
            return true;
        }
        for (Property<?> property : block.getStateDefinition().getProperties()) {
            String name = property.getName();
            if (!"half".equals(name) && !"part".equals(name)) continue;
            List<String> values = property.getPossibleValues().stream()
                    .map(value -> propertyNameUnchecked(property, value))
                    .toList();
            if (values.contains("upper") || values.contains("lower")
                    || values.contains("head") || values.contains("foot")) {
                return true;
            }
        }
        return false;
    }

    private static void validateStateMap(
            BlockState base, Map<String, String> values, BlockTransformRule rule, String path) {
        for (Map.Entry<String, String> entry : values.entrySet()) {
            Property<?> property = base.getBlock().getStateDefinition().getProperty(entry.getKey());
            if (property == null) {
                throw invalid(rule, path + " references missing property '" + entry.getKey() + "'");
            }
            if (property.getValue(entry.getValue()).isEmpty()) {
                throw invalid(rule, path + " has invalid value '" + entry.getValue()
                        + "' for property '" + entry.getKey() + "'");
            }
        }
    }

    private static IllegalArgumentException invalid(BlockTransformRule rule, String message) {
        return new IllegalArgumentException("rule " + rule.id() + ": " + message);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String propertyNameUnchecked(Property property, Comparable value) {
        return property.getName(value);
    }

    record CompiledSet(long generation, List<CompiledRule> rules, String error) {
        CompiledSet {
            rules = rules == null ? List.of() : List.copyOf(rules);
            error = error == null ? "" : error;
        }

        static CompiledSet empty(String error) {
            return new CompiledSet(Long.MIN_VALUE, List.of(), error);
        }

        boolean usable() {
            return error.isBlank();
        }
    }

    record CompiledRule(
            BlockTransformRule source,
            Block targetBlock,
            Block resultBlock,
            Item catalystItem,
            CompoundTag expectedComponents) {

        boolean itemMatches(ItemStack stack, HolderLookup.Provider registries) {
            if (stack == null || stack.isEmpty() || stack.getItem() != catalystItem) return false;
            CompoundTag components = components(stack, registries);
            String discriminator = source.catalyst().variantDiscriminator().trim();
            if (!discriminator.isEmpty()
                    && !discriminator.equals(AssetIdentity.variantDiscriminator(components.toString()))) {
                return false;
            }
            return expectedComponents == null || expectedComponents.equals(components);
        }

        boolean targetMatches(BlockState state) {
            if (state.getBlock() != targetBlock || state.hasBlockEntity()) return false;
            for (Map.Entry<String, String> required : source.target().state().entrySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(required.getKey());
                if (property == null || !required.getValue().equals(valueName(state, property))) return false;
            }
            return true;
        }

        BlockState resultState(BlockState targetState) {
            BlockState state = resultBlock.defaultBlockState();
            for (String propertyName : source.result().copyProperties()) {
                Property<?> from = targetState.getBlock().getStateDefinition().getProperty(propertyName);
                Property<?> to = state.getBlock().getStateDefinition().getProperty(propertyName);
                state = setValue(state, to, valueName(targetState, from));
            }
            for (Map.Entry<String, String> explicit : source.result().state().entrySet()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(explicit.getKey());
                state = setValue(state, property, explicit.getValue());
            }
            return state;
        }

        Map<String, String> targetStateValues(BlockState state) {
            Map<String, String> values = new LinkedHashMap<>();
            for (Property<?> property : state.getProperties()) {
                values.put(property.getName(), valueName(state, property));
            }
            return values;
        }

        private static CompoundTag components(ItemStack stack, HolderLookup.Provider registries) {
            Tag saved = stack.copyWithCount(1).save(registries);
            if (saved instanceof CompoundTag full && full.contains("components", Tag.TAG_COMPOUND)) {
                return full.getCompound("components").copy();
            }
            return new CompoundTag();
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static String valueName(BlockState state, Property property) {
            return property.getName(state.getValue(property));
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static BlockState setValue(BlockState state, Property property, String value) {
            Optional<? extends Comparable> parsed = property.getValue(value);
            if (parsed.isEmpty()) {
                throw new IllegalArgumentException("invalid block-state value " + value + " for " + property.getName());
            }
            return state.setValue(property, parsed.get());
        }
    }
}
