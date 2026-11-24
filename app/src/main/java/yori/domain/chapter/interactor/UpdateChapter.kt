package yori.domain.chapter.interactor

import yori.domain.chapter.ChapterRepository
import yori.domain.chapter.models.ChapterUpdate

class UpdateChapter(
    private val chapterRepository: ChapterRepository,
) {
    suspend fun await(chapter: ChapterUpdate) = chapterRepository.update(chapter)
    suspend fun awaitAll(chapters: List<ChapterUpdate>) = chapterRepository.updateAll(chapters)
}
