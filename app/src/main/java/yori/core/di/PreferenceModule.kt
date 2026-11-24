package yori.core.di

import android.app.Application
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.core.storage.AndroidStorageFolderProvider
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackPreferences
import eu.kanade.tachiyomi.network.NetworkPreferences
import org.koin.dsl.module
import yori.domain.backup.BackupPreferences
import yori.domain.base.BasePreferences
import yori.domain.download.DownloadPreferences
import yori.domain.library.LibraryPreferences
import yori.domain.recents.RecentsPreferences
import yori.domain.source.SourcePreferences
import yori.domain.storage.StoragePreferences
import yori.domain.ui.UiPreferences
import yori.domain.ui.settings.ReaderPreferences

fun preferenceModule(application: Application) = module {
    single<PreferenceStore> { AndroidPreferenceStore(application) }

    single { BasePreferences(get()) }

    single { SourcePreferences(get()) }

    single { TrackPreferences(get()) }

    single { UiPreferences(get()) }

    single { ReaderPreferences(get()) }

    single { RecentsPreferences(get()) }

    single { DownloadPreferences(get()) }

    single {
        NetworkPreferences(
            get(),
            BuildConfig.FLAVOR == "dev" || BuildConfig.DEBUG,
        )
    }

    single { SecurityPreferences(get()) }

    single { BackupPreferences(get()) }

    single { LibraryPreferences(get()) }

    single {
        PreferencesHelper(
            context = application,
            preferenceStore = get(),
        )
    }

    single {
        StoragePreferences(
            folderProvider = get<AndroidStorageFolderProvider>(),
            preferenceStore = get(),
        )
    }
}
