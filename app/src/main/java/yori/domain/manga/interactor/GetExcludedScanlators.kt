package yori.domain.manga.interactor

import yori.domain.manga.ExcludedScanlatorsRepository

class GetExcludedScanlators(
    private val repository: ExcludedScanlatorsRepository
) {
    suspend fun await(mangaId: Long) = repository.getExcludedScanlators(mangaId)

    fun subscribe(mangaId: Long) = repository.subscribeExcludedScanlators(mangaId)
}
