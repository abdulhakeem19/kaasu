package com.kaasu.app.domain.usecase.category

import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(category: Category): Long {
        return if (category.id == 0L) repository.insert(category)
        else { repository.update(category); category.id }
    }
}
