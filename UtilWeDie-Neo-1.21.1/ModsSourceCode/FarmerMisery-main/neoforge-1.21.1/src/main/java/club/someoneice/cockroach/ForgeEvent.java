package club.someoneice.cockroach;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;

@EventBusSubscriber(modid = ModMain.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ForgeEvent {
    @SubscribeEvent
    public static void addBrewingRecipe(RegisterBrewingRecipesEvent event) {
        var builder = event.getBuilder();
        builder.addRecipe(
                Ingredient.of(Items.POTION),
                Ingredient.of(ItemInit.ROACH_IN_BOTTLE.get()),
                ItemInit.KANGFU_XIN_YE.get().getDefaultInstance());
        builder.addRecipe(
                Ingredient.of(ItemInit.ROACH_IN_BOTTLE.get()),
                Ingredient.of(Items.GUNPOWDER),
                ItemInit.THROWABLE_ROACH_BOTTLE.get().getDefaultInstance());
    }
}
