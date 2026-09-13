package com.kaasu.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String?,
    val color: String?,
    val type: CategoryType,
    val monthlyBudgetInPaise: Long?,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)
