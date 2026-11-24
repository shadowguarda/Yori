package yori.domain.track.interactor

import yori.domain.track.TrackRepository

class GetTrack(
    private val trackRepository: TrackRepository,
) {
    suspend fun awaitAllByMangaId(mangaId: Long?) = mangaId?.let { trackRepository.getAllByMangaId(it) } ?: listOf()
}
