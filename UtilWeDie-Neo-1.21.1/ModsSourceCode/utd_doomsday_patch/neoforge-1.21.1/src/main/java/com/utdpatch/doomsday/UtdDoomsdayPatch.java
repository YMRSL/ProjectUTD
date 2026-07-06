package com.utdpatch.doomsday;

import com.utdpatch.doomsday.compat.SableReplayClientHandler;
import com.utdpatch.doomsday.compat.SableReplayPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod("utd_doomsday_patch")
public final class UtdDoomsdayPatch {
   public static final String MOD_ID = "utd_doomsday_patch";
   private static final Logger LOGGER = LogManager.getLogger("UTD Doomsday Patch");

   public UtdDoomsdayPatch(IEventBus modBus) {
      LOGGER.info("UTD Doomsday Patch @Mod constructed — registering runtime inventory repair (load-probe marker=utd-ctor)");
      NeoForge.EVENT_BUS.register(new UtdRuntimeInventoryRepair());
      modBus.addListener(this::registerPayloads);
      if (FMLEnvironment.dist.isClient()) {
         NeoForge.EVENT_BUS.register(new SableReplayClientHandler.TickFlusher());
      }
   }

   private void registerPayloads(RegisterPayloadHandlersEvent event) {
      // sable x Flashback bridge carrier.
      // The handler calls SableReplayClientHandler.handleFromNeoForge which:
      //   • logs thread name + level/connection state every call (grep [SABLE-REPLAY] HANDLER)
      //   • dispatches immediately when level+connection are ready (same call frame
      //     as Flashback's FlashbackRawCustomPayload inline dispatch, preserving order)
      //   • falls back to tick-flusher queue when world is not yet ready
      event.registrar("1").optional().playToClient(
            SableReplayPayload.TYPE,
            SableReplayPayload.STREAM_CODEC,
            (payload, context) -> {
               if (FMLEnvironment.dist.isClient()) {
                  SableReplayClientHandler.handleFromNeoForge(payload, context);
               }
            });
   }
}
