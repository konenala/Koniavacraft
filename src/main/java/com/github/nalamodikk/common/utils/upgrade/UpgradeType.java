package com.github.nalamodikk.common.utils.upgrade;

public enum UpgradeType {
    SPEED("speed"),
    EFFICIENCY("efficiency");

    private final String name;

    UpgradeType(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return name;
    }
}
