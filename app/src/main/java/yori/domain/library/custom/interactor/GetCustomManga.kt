package yori.domain.library.custom.interactor

import yori.domain.library.custom.CustomMangaRepository

class GetCustomManga(
    private val customMangaRepository: CustomMangaRepository,
) {
    fun subscribeAll() = customMangaRepository.subscribeAll()

    suspend fun getAll() = customMangaRepository.getAll()
}
