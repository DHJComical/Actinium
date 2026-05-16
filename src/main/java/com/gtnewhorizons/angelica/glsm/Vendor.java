package com.gtnewhorizons.angelica.glsm;

/**
 * GPU vendor detection enum.
 * Used for driver-specific workarounds and optimizations.
 */
public enum Vendor {
    AMD("amd"),
    INTEL("intel"),
    MESA("mesa"),
    NVIDIA("nvidia"),
    UNKNOWN("");

    final String[] names;

    Vendor(String... names) {
        this.names = names;
    }

    public static Vendor getVendor(String vendorString) {
        if (vendorString == null) return UNKNOWN;
        final String lower = vendorString.toLowerCase();
        for (var v : values()) {
            for (var name : v.names) {
                if (lower.contains(name)) return v;
            }
        }
        return UNKNOWN;
    }
}
