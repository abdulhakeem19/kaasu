package com.kaasu.app.domain.usecase.transaction

import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository
) {
    operator fun invoke(): Flow<List<Category>> = repository.getAllActive()
}
