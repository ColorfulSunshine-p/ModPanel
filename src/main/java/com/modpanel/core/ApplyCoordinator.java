package com.modpanel.core;

import com.modpanel.adapter.ConfigAdapter;
import com.modpanel.model.ApplyResult;
import com.modpanel.model.ConfigChange;
import com.modpanel.model.ConfigEntry;
import com.modpanel.model.ConfigSource;
import com.modpanel.model.ReloadRequirement;
import com.modpanel.persistence.BackupService;
import com.modpanel.validation.DefaultValidator;
import com.modpanel.validation.ValidationResult;
import com.modpanel.validation.ValueValidator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class ApplyCoordinator {
    private final ValueValidator validator = new DefaultValidator();
    private final BackupService backupService = new BackupService();

    public ApplyResult apply(ConfigAdapter adapter, ConfigSource source, ChangeSet changeSet, List<ConfigEntry> entries) {
        List<ConfigChange> changes = changeSet.toChanges();
        for (ConfigChange c : changes) {
            ConfigEntry e = findEntry(entries, c.entryId());
            if (e == null) continue;
            ValidationResult vr = adapter.validate(e, c.proposedValue());
            if (!vr.valid()) vr = validator.validate(e, c.proposedValue());
            if (!vr.valid()) {
                com.modpanel.diagnostics.Diagnostics.report(com.modpanel.diagnostics.ErrorCategory.VALIDATION,
                        "Rejected " + c.entryId() + ": " + vr.message(), null);
                return new ApplyResult(false, "Invalid value: " + vr.message());
            }
        }

        Path backup = backupService.backup(source.getSourceId(), Paths.get(source.getLocation()));
        ApplyResult result = adapter.apply(source, changes);
        if (!result.success()) {
            com.modpanel.diagnostics.Diagnostics.report(
                    result.conflict() ? com.modpanel.diagnostics.ErrorCategory.CONFLICT
                            : com.modpanel.diagnostics.ErrorCategory.WRITE,
                    "Apply failed for " + source.getSourceId() + ": " + result.message(), null);
            return new ApplyResult(false, result.message(), backup == null ? null : backup.toString(),
                    result.reload(), result.conflict());
        }
        changeSet.clear();
        ReloadRequirement reload = adapter.reloadRequirement(source, changes);
        return new ApplyResult(true, result.message(), backup == null ? null : backup.toString(), reload, false);
    }

    public ReloadRequirement reloadRequirement(ConfigAdapter adapter, ConfigSource source, List<ConfigChange> changes) {
        return adapter.reloadRequirement(source, changes);
    }

    private ConfigEntry findEntry(List<ConfigEntry> entries, String id) {
        for (ConfigEntry e : entries) if (e.getId().equals(id)) return e;
        return null;
    }
}
