package yori.domain.extension.repo.interactor

import yori.domain.extension.repo.ExtensionRepoRepository

class GetExtensionRepoCount(
    private val extensionRepoRepository: ExtensionRepoRepository
) {
    fun subscribe() = extensionRepoRepository.getCount()
}
