package com.giapha.enums;

public enum RelationshipType {
    SPOUSE("Vợ chồng"),
    PARENT_CHILD("Cha mẹ - Con cái");

    private final String displayName;

    RelationshipType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
