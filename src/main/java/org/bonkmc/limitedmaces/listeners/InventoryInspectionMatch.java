package org.bonkmc.limitedmaces.listeners;

record InventoryInspectionMatch(boolean hasMatch, boolean hasUntaggedMace) {
    static final InventoryInspectionMatch NONE = new InventoryInspectionMatch(false, false);
}
