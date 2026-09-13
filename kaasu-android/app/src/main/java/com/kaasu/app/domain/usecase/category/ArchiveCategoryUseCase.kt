package com.kaasu.app.domain.usecase.category

import com.kaasu.app.domain.repository.CategoryRepository
import javax.inject.Inject

class ArchiveCategoryUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    suspend operator fun invoke(id: Long) = repository.archive(id)
}
