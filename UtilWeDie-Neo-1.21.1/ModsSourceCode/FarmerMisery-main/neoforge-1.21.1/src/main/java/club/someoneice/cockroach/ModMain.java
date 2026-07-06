package club.someoneice.cockroach;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(ModMain.MODID)
public final class ModMain {
    public static final String MODID = "farmer_misery";

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    static {
        TABS.register(MODID + "_tab", () -> CreativeModeTab.builder()
                .title(Component.translatable(MODID + ".tab"))
                .icon(() -> ItemInit.ROACH_IN_BOTTLE.get().getDefaultInstance())
                .displayItems((itemDisplayParameters, output) ->
                        ItemInit.ITEMS.getEntries().stream().map(Supplier::get).forEach(output::accept))
                .build()
        );
    }

    public ModMain(IEventBus bus, ModContainer container) {
        ItemInit.ITEMS.register(bus);
        EntityInit.ENTITIES.register(bus);
        TABS.register(bus);
    }
}
