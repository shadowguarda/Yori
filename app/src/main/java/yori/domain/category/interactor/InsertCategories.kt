package yori.domain.category.interactor

import eu.kanade.tachiyomi.data.database.models.Category
import yori.domain.category.CategoryRepository

class InsertCategories(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun await(categories: List<Category>) = categoryRepository.insertBulk(categories)
    suspend fun awaitOne(category: Category) = categoryRepository.insert(category)
}
