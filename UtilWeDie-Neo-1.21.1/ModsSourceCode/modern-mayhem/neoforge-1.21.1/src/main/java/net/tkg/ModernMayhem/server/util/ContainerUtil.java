package net.tkg.ModernMayhem.server.util;

public interface ContainerUtil {
    default public int getBackpackSize() {
        return this.getSlotPerLine() * this.getNumberOfLine();
    }

    public int getSlotPerLine();

    public int getNumberOfLine();
}

