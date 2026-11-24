package yori.core.di

import org.koin.dsl.module
import yori.data.category.CategoryRepositoryImpl
import yori.data.chapter.ChapterRepositoryImpl
import yori.data.extension.repo.ExtensionRepoRepositoryImpl
import yori.data.history.HistoryRepositoryImpl
import yori.data.library.custom.CustomMangaRepositoryImpl
import yori.data.manga.ExcludedScanlatorsRepositoryImpl
import yori.data.manga.MangaRepositoryImpl
import yori.data.source.browse.filter.SavedSearchRepositoryImpl
import yori.data.track.TrackRepositoryImpl
import yori.domain.category.CategoryRepository
import yori.domain.category.interactor.DeleteCategories
import yori.domain.category.interactor.GetCategories
import yori.domain.category.interactor.InsertCategories
import yori.domain.category.interactor.SetMangaCategories
import yori.domain.category.interactor.UpdateCategories
import yori.domain.chapter.ChapterRepository
import yori.domain.chapter.interactor.DeleteChapter
import yori.domain.chapter.interactor.GetAvailableScanlators
import yori.domain.chapter.interactor.GetChapter
import yori.domain.chapter.interactor.InsertChapter
import yori.domain.chapter.interactor.UpdateChapter
import yori.domain.extension.interactor.TrustExtension
import yori.domain.extension.repo.ExtensionRepoRepository
import yori.domain.extension.repo.interactor.CreateExtensionRepo
import yori.domain.extension.repo.interactor.DeleteExtensionRepo
import yori.domain.extension.repo.interactor.GetExtensionRepo
import yori.domain.extension.repo.interactor.GetExtensionRepoCount
import yori.domain.extension.repo.interactor.ReplaceExtensionRepo
import yori.domain.extension.repo.interactor.UpdateExtensionRepo
import yori.domain.history.HistoryRepository
import yori.domain.history.interactor.GetHistory
import yori.domain.history.interactor.UpsertHistory
import yori.domain.library.custom.CustomMangaRepository
import yori.domain.library.custom.interactor.CreateCustomManga
import yori.domain.library.custom.interactor.DeleteCustomManga
import yori.domain.library.custom.interactor.GetCustomManga
import yori.domain.library.custom.interactor.RelinkCustomManga
import yori.domain.manga.ExcludedScanlatorsRepository
import yori.domain.manga.MangaRepository
import yori.domain.manga.interactor.GetExcludedScanlators
import yori.domain.manga.interactor.GetLibraryManga
import yori.domain.manga.interactor.GetManga
import yori.domain.manga.interactor.InsertManga
import yori.domain.manga.interactor.UpdateManga
import yori.domain.manga.interactor.SetExcludedScanlators
import yori.domain.recents.interactor.GetRecents
import yori.domain.source.browse.filter.FilterSerializer
import yori.domain.source.browse.filter.SavedSearchRepository
import yori.domain.source.browse.filter.interactor.DeleteSavedSearch
import yori.domain.source.browse.filter.interactor.GetSavedSearch
import yori.domain.source.browse.filter.interactor.InsertSavedSearch
import yori.domain.track.TrackRepository
import yori.domain.track.interactor.DeleteTrack
import yori.domain.track.interactor.GetTrack
import yori.domain.track.interactor.InsertTrack

fun domainModule() = module {
    factory { TrustExtension(get(), get()) }

    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    factory { DeleteCategories(get()) }
    factory { GetCategories(get()) }
    factory { InsertCategories(get()) }
    factory { UpdateCategories(get()) }

    single<ExtensionRepoRepository> { ExtensionRepoRepositoryImpl(get()) }
    factory { CreateExtensionRepo(get()) }
    factory { DeleteExtensionRepo(get()) }
    factory { GetExtensionRepo(get()) }
    factory { GetExtensionRepoCount(get()) }
    factory { ReplaceExtensionRepo(get()) }
    factory { UpdateExtensionRepo(get(), get()) }

    single<CustomMangaRepository> { CustomMangaRepositoryImpl(get()) }
    factory { CreateCustomManga(get()) }
    factory { DeleteCustomManga(get()) }
    factory { GetCustomManga(get()) }
    factory { RelinkCustomManga(get()) }

    single<MangaRepository> { MangaRepositoryImpl(get()) }
    factory { GetManga(get()) }
    factory { GetLibraryManga(get()) }
    factory { InsertManga(get()) }
    factory { UpdateManga(get()) }

    single<ExcludedScanlatorsRepository> { ExcludedScanlatorsRepositoryImpl(get()) }
    factory { GetExcludedScanlators(get()) }
    factory { SetExcludedScanlators(get()) }

    factory { SetMangaCategories(get()) }

    single<ChapterRepository> { ChapterRepositoryImpl(get()) }
    factory { DeleteChapter(get()) }
    factory { GetAvailableScanlators(get()) }
    factory { GetChapter(get()) }
    factory { InsertChapter(get()) }
    factory { UpdateChapter(get()) }

    single<HistoryRepository> { HistoryRepositoryImpl(get()) }
    factory { GetHistory(get()) }
    factory { UpsertHistory(get()) }

    factory { GetRecents(get(), get()) }

    single<TrackRepository> { TrackRepositoryImpl(get()) }
    factory { DeleteTrack(get()) }
    factory { GetTrack(get()) }
    factory { InsertTrack(get()) }

    single<SavedSearchRepository> { SavedSearchRepositoryImpl(get()) }
    factory { DeleteSavedSearch(get()) }
    factory { GetSavedSearch(get()) }
    factory { InsertSavedSearch(get()) }
    factory { FilterSerializer() }
}
