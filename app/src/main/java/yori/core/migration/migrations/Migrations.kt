package yori.core.migration.migrations

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import yori.core.migration.Migration

val migrations: ImmutableList<Migration> = persistentListOf(
    // Always run
    SetupAppUpdateMigration(),
    SetupBackupCreateMigration(),
    SetupExtensionUpdateMigration(),
    SetupLibraryUpdateMigration(),

    // Yori fork
    CutoutMigration(),
    ExtensionInstallerEnumMigration(),
    RepoJsonMigration(),
    ThePurgeMigration(),
)
