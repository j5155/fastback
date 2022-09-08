package net.pcal.fastback;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Config;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public record WorldConfig(
        String worldUuid,
        boolean isBackupEnabled,
        boolean isShutdownBackupEnabled,
        boolean isRemoteBackupEnabled,
        String getRemotePushUri) {

    public static final String WORLD_UUID_PATH = "world.uuid";

    public String getRemoteName() {
        return REMOTE_NAME;
    }

    public boolean isUuidCheckEnabled() {
        return true;
    }

    public boolean isTempBranchCleanupEnabled() {
        return true;
    }

    public boolean isFileRemoteTempBranchCleanupEnabled() {
        return true;
    }

    public boolean isSmartPushEnabled() {
        return true;
    }

    private static final String REMOTE_NAME = "origin";
    private static final String CONFIG_SECTION = "fastback";
    private static final String CONFIG_BACKUP_ENABLED = "backup-enabled";
    private static final String CONFIG_SHUTDOWN_BACKUP_ENABLED = "shutdown-backup-enabled";
    private static final String CONFIG_REMOTE_BACKUP_ENABLED = "remote-backup-enabled";

    public static WorldConfig load(Path worldSaveDir) throws IOException {
        try (Git git = Git.open(worldSaveDir.toFile())) {
            return load(worldSaveDir, git.getRepository().getConfig());
        }
    }

    public static WorldConfig load(Path worldSaveDir, Config gitConfig) throws IOException {
        return new WorldConfig(
                getWorldUuid(worldSaveDir),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, false),
                gitConfig.getBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, false),
                gitConfig.getString("remote", REMOTE_NAME, "url")
        );
    }

    // REMEMBER TO CALL config.save() YOURSELF!!

    public static void setRemoteUrl(Config gitConfig, String url) {
        gitConfig.setString("remote", REMOTE_NAME, "url", url);
    }

    public static void setBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_BACKUP_ENABLED, value);
    }

    public static void setShutdownBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_SHUTDOWN_BACKUP_ENABLED, value);
    }

    public static void setRemoteBackupEnabled(Config gitConfig, boolean value) {
        gitConfig.setBoolean(CONFIG_SECTION, null, CONFIG_REMOTE_BACKUP_ENABLED, value);
    }

    public static String getWorldUuid(Path worldSaveDir) throws IOException {
        return Files.readString(worldSaveDir.resolve(Path.of(WORLD_UUID_PATH))).trim();
    }

    public static void ensureWorldHasUuid(final Path worldSaveDir, final Loggr logger) throws IOException {
        final Path worldUuidpath = worldSaveDir.resolve(WORLD_UUID_PATH);
        if (!worldUuidpath.toFile().exists()) {
            final String newUuid = UUID.randomUUID().toString();
            try (final FileWriter fw = new FileWriter(worldUuidpath.toFile())) {
                fw.append(newUuid);
                fw.append('\n');
            }
            logger.info("Generated new world.uuid " + newUuid);
        }
    }

}


