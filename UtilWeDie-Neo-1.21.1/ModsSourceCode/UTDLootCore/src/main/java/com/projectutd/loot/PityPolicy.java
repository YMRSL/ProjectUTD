package com.projectutd.loot;

/** Pure policy functions kept independent of Minecraft so they are unit-testable offline. */
final class PityPolicy {
    private PityPolicy() {}

    static double tier4SoftChance(int missCount) {
        return switch (missCount) {
            case 7 -> 0.10D;
            case 8 -> 0.20D;
            case 9 -> 0.35D;
            default -> 0.0D;
        };
    }

    static boolean tier4HardGuarantee(int missCount) {
        return missCount >= 10;
    }

    static double tier5SoftChance(int missCount) {
        return switch (missCount) {
            case 15 -> 0.01D;
            case 16 -> 0.02D;
            case 17 -> 0.03D;
            case 18 -> 0.05D;
            case 19 -> 0.08D;
            default -> 0.0D;
        };
    }

    static boolean tier5HardGuarantee(int missCount) {
        return missCount >= 20;
    }
}
