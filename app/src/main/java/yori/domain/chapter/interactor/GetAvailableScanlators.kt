package yori.domain.chapter.interactor

import yori.domain.chapter.ChapterRepository

class GetAvailableScanlators(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun await(mangaId: Long) = chapterRepository.getScanlatorsByChapter(mangaId)
    fun subscribe(mangaId: Long) = chapterRepository.getScanlatorsByChapterAsFlow(mangaId)
}
