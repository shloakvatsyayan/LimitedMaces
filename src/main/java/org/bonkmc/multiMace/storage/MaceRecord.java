package org.bonkmc.multiMace.storage;

import org.bukkit.Location;

import java.util.UUID;

public final class MaceRecord {
    public UUID id;

    public UUID createdBy;
    public String createdByName;
    public long createdAt;

    public UUID lastHolder;
    public String lastHolderName;

    public String lastWorld;
    public double lastX;
    public double lastY;
    public double lastZ;

    public long lastSeenAt;
    public String status;
    public boolean isUntracked;

    public void setLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.lastWorld = loc.getWorld().getName();
        this.lastX = loc.getX();
        this.lastY = loc.getY();
        this.lastZ = loc.getZ();
    }
}
