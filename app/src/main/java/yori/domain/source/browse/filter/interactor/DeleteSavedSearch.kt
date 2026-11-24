package yori.domain.source.browse.filter.interactor

import yori.domain.source.browse.filter.SavedSearchRepository

class DeleteSavedSearch(
    private val repository: SavedSearchRepository,
) {
    suspend fun await(searchId: Long) = repository.deleteById(searchId)
}
