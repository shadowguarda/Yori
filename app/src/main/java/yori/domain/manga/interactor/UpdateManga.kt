package yori.domain.manga.interactor

import yori.domain.manga.MangaRepository
import yori.domain.manga.models.MangaUpdate

class UpdateManga (
    private val mangaRepository: MangaRepository,
) {
    suspend fun await(update: MangaUpdate) = mangaRepository.update(update)
    suspend fun awaitAll(updates: List<MangaUpdate>) = mangaRepository.updateAll(updates)
}
