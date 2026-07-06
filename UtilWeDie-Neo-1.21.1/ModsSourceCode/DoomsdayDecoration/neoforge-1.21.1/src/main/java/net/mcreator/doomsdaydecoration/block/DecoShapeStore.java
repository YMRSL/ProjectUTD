package net.mcreator.doomsdaydecoration.block;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Restores the per-block hand-written {@code VoxelShape}s the data-driven port
 * dropped (PORT_REPORT §7 gap 1), recovered into {@code doomsday_decoration_shapes.json}.
 *
 * <p><b>getShape</b> (selection outline) always uses the exact recovered silhouette.
 * Collision is chosen automatically by footprint, not by "vehicle":
 * <ul>
 *   <li><b>2-D multi-cell blocks</b> (footprint has a diagonal cell dx≠0 AND dz≠0 —
 *   vehicles, hesco, wc…): the owner collides only its own cell and an invisible
 *   {@link CollisionFillerBlock} is placed in every other footprint cell. Real
 *   blocks in every cell give clean per-cell physics (proper onGround / jump), tile
 *   cell-aligned (no catch / suck), and are gap-free (vanilla skips diagonal cells
 *   for a lone oversized shape → clip-in + hover). Cells the shape barely touches
 *   (a thin tail hook) are skipped so a tiny protrusion is not a whole-cell wall.</li>
 *   <li><b>Single-cell / 1-D blocks</b> (no diagonal cell — partitions, pipes): the
 *   engine handles their exact shape perfectly, so collision stays the exact
 *   silhouette (tight) and no fillers are placed.</li>
 * </ul>
 * The filler shape is a cell-aligned full box grounded to the floor up to the cell
 * height ({@code top}) — <b>cached</b> per blockstate (a dynamic per-cell shape
 * jitters). No entity mixin, so other mods' crawl (TaCZ / ParCool) is untouched.
 */
public final class DecoShapeStore {
    private DecoShapeStore() {}

    /** Smallest cell-coverage dimension (px) below which a cell is a thin protrusion to skip. */
    private static final float MIN_FILL_PX = 5.0F;

    private static final class Entry {
        final boolean facing;
        final boolean oversized;
        final VoxelShape[] display;      // exact silhouette (getShape)
        final VoxelShape[] collision;    // getCollisionShape (own cell for 2-D, exact for 1-D)
        final int[][][] fillers;         // per facing: array of {dx,dz,topPx}; null if none
        final int lift;                  // placement lift in cells (shape dips below its cell), 0 = none
        final boolean sunken;            // shape floor is 1 cell below (fillers/own cell extend to -16)

        Entry(boolean facing, boolean oversized, VoxelShape[] display, VoxelShape[] collision,
              int[][][] fillers, int lift, boolean sunken) {
            this.facing = facing;
            this.oversized = oversized;
            this.display = display;
            this.collision = collision;
            this.fillers = fillers;
            this.lift = lift;
            this.sunken = sunken;
        }
    }

    private static int facingIndex(Direction facing) {
        if (facing == null) return 0;
        switch (facing) {
            case EAST:  return 1;
            case SOUTH: return 2;
            case WEST:  return 3;
            case NORTH:
            default:    return 0;
        }
    }

    private static final Map<Block, Entry> BY_BLOCK = new IdentityHashMap<>();
    private static JsonObject raw;

    private static synchronized void ensureLoaded() {
        if (raw != null) return;
        try (InputStream in = DecoShapeStore.class.getResourceAsStream("/doomsday_decoration_shapes.json")) {
            raw = in == null
                    ? new JsonObject()
                    : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            raw = new JsonObject();
        }
    }

    public static void apply(Block block, String name) {
        ensureLoaded();
        if (!raw.has(name)) return;
        JsonObject o = raw.getAsJsonObject(name);
        boolean facing = o.get("facing").getAsBoolean();
        boolean oversized = o.get("overhang").getAsBoolean();
        JsonObject shapes = o.getAsJsonObject("shapes");
        JsonObject cells = oversized && o.has("cells") ? o.getAsJsonObject("cells") : null;

        String[] dirs = facing ? new String[]{"north", "east", "south", "west"} : new String[]{"all"};
        int n = dirs.length;
        VoxelShape[] display = new VoxelShape[n];
        VoxelShape[] collision = new VoxelShape[n];
        int[][][] fillers = new int[n][][];

        boolean multiCell = cells != null && hasDiagonalCell(cells, dirs);
        // The shape floor: some blocks (hummers, ambulances…) dip a cell below their
        // own cell — MC models can't be shifted up (max Y=32), so on placement they
        // are lifted that many cells to sit on the ground, and their fillers / own
        // cell extend down to the floor so the sunken part (wheels) still collides.
        float minY = 0;
        for (String d : dirs) {
            for (JsonElement el : shapes.getAsJsonArray(d)) {
                minY = Math.min(minY, el.getAsJsonArray().get(1).getAsFloat());
            }
        }
        boolean sunken = minY < 0;
        int lift = sunken ? (int) Math.ceil(-minY / 16.0) : 0;
        int floorPx = sunken ? -16 : 0;

        for (int i = 0; i < n; i++) {
            display[i] = build(shapes.getAsJsonArray(dirs[i]));
            if (multiCell) {
                JsonArray cellArr = cells.getAsJsonArray(dirs[i]);
                collision[i] = ownerCell(cellArr, floorPx);   // own cell, extend down if sunken
                fillers[i] = fillerCells(cellArr);            // other cells, skip thin protrusions
            } else {
                collision[i] = display[i];                    // single-cell / 1-D: exact, tight
                fillers[i] = null;
            }
        }
        BY_BLOCK.put(block, new Entry(facing, oversized, display, collision, fillers, lift, sunken));
    }

    private static VoxelShape build(JsonArray boxes) {
        if (boxes == null || boxes.isEmpty()) return Shapes.empty();
        VoxelShape shape = null;
        for (JsonElement el : boxes) {
            JsonArray b = el.getAsJsonArray();
            VoxelShape box = Block.box(b.get(0).getAsDouble(), b.get(1).getAsDouble(), b.get(2).getAsDouble(),
                    b.get(3).getAsDouble(), b.get(4).getAsDouble(), b.get(5).getAsDouble());
            shape = shape == null ? box : Shapes.or(shape, box);
        }
        return shape == null ? Shapes.empty() : shape;
    }

    private static boolean hasDiagonalCell(JsonObject cells, String[] dirs) {
        for (String d : dirs) {
            for (JsonElement el : cells.getAsJsonArray(d)) {
                JsonObject c = el.getAsJsonObject();
                if (c.get("dx").getAsInt() != 0 && c.get("dz").getAsInt() != 0) return true;
            }
        }
        return false;
    }

    /** Owner (0,0) cell collision: cell-aligned full box from {@code floorPx} up to the cell top. */
    private static VoxelShape ownerCell(JsonArray cellArr, int floorPx) {
        for (JsonElement el : cellArr) {
            JsonObject c = el.getAsJsonObject();
            if (c.get("dx").getAsInt() == 0 && c.get("dz").getAsInt() == 0) {
                return Block.box(0, floorPx, 0, 16, cellTop(c.getAsJsonArray("boxes")), 16);
            }
        }
        return Shapes.block();
    }

    /** Filler cells = footprint minus owner, minus thin protrusions. {dx,dz,topPx}. */
    private static int[][] fillerCells(JsonArray cellArr) {
        List<int[]> out = new ArrayList<>();
        for (JsonElement el : cellArr) {
            JsonObject c = el.getAsJsonObject();
            int dx = c.get("dx").getAsInt();
            int dz = c.get("dz").getAsInt();
            if (dx == 0 && dz == 0) continue;
            JsonArray boxes = c.getAsJsonArray("boxes");
            if (boxes.isEmpty()) continue;
            JsonArray b0 = boxes.get(0).getAsJsonArray();
            float w = b0.get(3).getAsFloat() - b0.get(0).getAsFloat();
            float d = b0.get(5).getAsFloat() - b0.get(2).getAsFloat();
            if (Math.min(w, d) < MIN_FILL_PX) continue;    // thin protrusion — no filler
            out.add(new int[]{dx, dz, cellTop(boxes)});
        }
        return out.toArray(new int[0][]);
    }

    /** Max top (px, rounded, clamped 1..32) across a cell's boxes. */
    private static int cellTop(JsonArray boxes) {
        float top = 1;
        for (JsonElement el : boxes) top = Math.max(top, el.getAsJsonArray().get(4).getAsFloat());
        return (int) Math.max(1, Math.min(32, Math.round(top)));
    }

    /** Exact silhouette (getShape / selection). Null if the block has no recovered shape. */
    public static VoxelShape shape(Block block, Direction facing) {
        Entry e = BY_BLOCK.get(block);
        return e == null ? null : e.display[e.facing ? facingIndex(facing) : 0];
    }

    /** Collision shape (own cell for 2-D multi-cell blocks; exact for 1-D). */
    public static VoxelShape collisionShape(Block block, Direction facing) {
        Entry e = BY_BLOCK.get(block);
        return e == null ? null : e.collision[e.facing ? facingIndex(facing) : 0];
    }

    /** Footprint filler cells ({dx,dz,top}) for {@code block} at {@code facing}, or null. */
    public static int[][] fillers(Block block, Direction facing) {
        Entry e = BY_BLOCK.get(block);
        if (e == null || e.fillers == null) return null;
        return e.fillers[e.facing ? facingIndex(facing) : 0];
    }

    public static boolean has(Block block) {
        return BY_BLOCK.containsKey(block);
    }

    public static boolean oversized(Block block) {
        Entry e = BY_BLOCK.get(block);
        return e != null && e.oversized;
    }

    /** Cells to lift the block on placement so its below-cell part sits on the ground (0 = none). */
    public static int lift(Block block) {
        Entry e = BY_BLOCK.get(block);
        return e == null ? 0 : e.lift;
    }

    /** True if the block's shape dips 1 cell below its own cell (fillers extend down to -16). */
    public static boolean sunken(Block block) {
        Entry e = BY_BLOCK.get(block);
        return e != null && e.sunken;
    }

    /** DIAGNOSTIC: registration summary at client setup. */
    public static void logDiagnostics() {
        org.slf4j.Logger log = com.mojang.logging.LogUtils.getLogger();
        int oversized = 0, multi = 0;
        for (Entry e : BY_BLOCK.values()) {
            if (e.oversized) oversized++;
            if (e.fillers != null && e.fillers[0] != null && e.fillers[0].length > 0) multi++;
        }
        log.info("[DD-SHAPE] {} blocks, {} oversized, {} multi-cell(filler)", BY_BLOCK.size(), oversized, multi);
    }
}
