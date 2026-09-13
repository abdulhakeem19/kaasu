package com.kaasu.app.data.mapper

import com.kaasu.app.core.database.entity.CategoryEntity
import com.kaasu.app.domain.model.Category
import com.kaasu.app.domain.model.CategoryType

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = runCatching { CategoryType.valueOf(type) }.getOrDefault(CategoryType.EXPENSE),
    monthlyBudgetInPaise = monthlyBudgetInPaise,
    isDefault = isDefault,
    isArchived = isArchived
)

fun Category.toEntity(
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.name,
    monthlyBudgetInPaise = monthlyBudgetInPaise,
    isDefault = isDefault,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt
)
