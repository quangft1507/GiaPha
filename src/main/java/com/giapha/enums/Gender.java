package com.giapha.enums;

public enum Gender {
    NAM("Nam"),
    NU("Nữ");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
