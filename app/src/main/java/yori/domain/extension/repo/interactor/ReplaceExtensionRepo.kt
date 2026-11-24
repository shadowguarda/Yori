package yori.domain.extension.repo.interactor

import yori.domain.extension.repo.ExtensionRepoRepository
import yori.domain.extension.repo.model.ExtensionRepo

class ReplaceExtensionRepo(
    private val extensionRepoRepository: ExtensionRepoRepository
) {
    suspend fun await(repo: ExtensionRepo) {
        extensionRepoRepository.replaceRepository(repo)
    }
}
