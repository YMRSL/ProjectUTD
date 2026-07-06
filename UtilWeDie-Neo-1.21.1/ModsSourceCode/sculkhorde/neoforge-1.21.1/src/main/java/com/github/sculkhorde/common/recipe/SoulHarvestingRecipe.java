package com.github.sculkhorde.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public class SoulHarvestingRecipe implements Recipe<SingleRecipeInput> {

    private final NonNullList<Ingredient> inputItems;
    private final ItemStack output;
    private final int healthRequired;

    public SoulHarvestingRecipe(NonNullList<Ingredient> inputItems, ItemStack output, int healthRequired) {
        this.inputItems = inputItems;
        this.output = output;
        this.healthRequired = healthRequired;
    }

    @Override
    public boolean matches(SingleRecipeInput inputIn, Level levelIn) {
        if(levelIn.isClientSide()) { return false; }

        return inputItems.get(0).test(inputIn.getItem(0));
    }

    @Override
    public NonNullList<Ingredient> getIngredients()
    {
        return inputItems;
    }

    @Override
    public ItemStack assemble(SingleRecipeInput inputIn, HolderLookup.Provider registries) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return output.copy();
    }

    public int getHealthRequired() {
        return healthRequired;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public static class Type implements RecipeType<SoulHarvestingRecipe> {
        public static final Type INSTANCE = new Type();

        public static final String ID = "sculkhorde:soul_harvesting";
    }

    public static class Serializer implements RecipeSerializer<SoulHarvestingRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<SoulHarvestingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC_NONEMPTY
                        .listOf()
                        .fieldOf("ingredients")
                        .xmap(ingredients -> {
                            NonNullList<Ingredient> list = NonNullList.create();
                            list.addAll(ingredients);
                            return list;
                        }, list -> list)
                        .forGetter(recipe -> recipe.inputItems),
                ItemStack.STRICT_CODEC.fieldOf("output").forGetter(recipe -> recipe.output),
                com.mojang.serialization.Codec.INT.optionalFieldOf("healthRequired", 0).forGetter(recipe -> recipe.healthRequired)
        ).apply(instance, SoulHarvestingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, SoulHarvestingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<SoulHarvestingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SoulHarvestingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static SoulHarvestingRecipe fromNetwork(RegistryFriendlyByteBuf bufferIn) {
            int size = bufferIn.readInt();
            NonNullList<Ingredient> inputItems = NonNullList.withSize(size, Ingredient.EMPTY);
            for (int i = 0; i < inputItems.size(); i++) {
                inputItems.set(i, Ingredient.CONTENTS_STREAM_CODEC.decode(bufferIn));
            }
            ItemStack output = ItemStack.STREAM_CODEC.decode(bufferIn);
            int healthRequired = bufferIn.readInt();
            return new SoulHarvestingRecipe(inputItems, output, healthRequired);
        }

        private static void toNetwork(RegistryFriendlyByteBuf bufferIn, SoulHarvestingRecipe recipeIn) {
            bufferIn.writeInt(recipeIn.inputItems.size());
            for (Ingredient ingredient : recipeIn.getIngredients()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(bufferIn, ingredient);
            }
            ItemStack.STREAM_CODEC.encode(bufferIn, recipeIn.output);
            bufferIn.writeInt(recipeIn.getHealthRequired());
        }
    }
}
