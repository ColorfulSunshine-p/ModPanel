package com.modpanel.model;

public record ApplyResult(boolean success, String message, String backupPath, ReloadRequirement reload, boolean conflict) {
    public ApplyResult(boolean success, String message) {
        this(success, message, null, ReloadRequirement.none(), false);
    }
}
