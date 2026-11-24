package yori.domain.track.interactor

import yori.domain.track.TrackRepository

class DeleteTrack(
    private val trackRepository: TrackRepository,
) {
    suspend fun awaitForManga(mangaId: Long, syncId: Long) = trackRepository.deleteForManga(mangaId, syncId)
}
