package com.giapha.enums;

public enum CareType {
    CUNG_DUONG("Cúng dường"),
    HAU_SU("Lo hậu sự"),
    CHIU_TANG("Chịu tang");

    private final String displayName;

    CareType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
