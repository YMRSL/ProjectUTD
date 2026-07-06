package net.mcreator.doomsdaydecoration.block;

/**
 * Marker for decoration blocks that carry a recovered hand-written VoxelShape
 * (see {@link DecoShapeStore}). Lets the entity-collision mixin fast-reject the
 * vast majority of blocks with a single {@code instanceof} before doing the
 * cheaper-but-not-free oversized lookup.
 */
public interface DecoShaped {
    /**
     * @return true if this block's recovered collision shape overhangs its own
     *         1×1×1 cell, so its protrusion into neighbouring (often empty) cells
     *         must be contributed to collision from those cells as well. Vanilla
     *         only queries a block's shape from the block's own cell, so without
     *         this the overhang would be walk-through (the original 1.20.1
     *         behaviour). The collision mixin uses this to make oversized
     *         decorations true multi-cell solid obstacles.
     */
    boolean ddOversized();
}
