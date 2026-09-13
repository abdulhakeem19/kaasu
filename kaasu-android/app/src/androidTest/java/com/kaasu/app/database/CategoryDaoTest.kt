package com.kaasu.app.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaasu.app.core.database.KaasuDatabase
import com.kaasu.app.core.database.entity.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: KaasuDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            KaasuDatabase::class.java
        ).build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndRetrieveCategory() = runTest {
        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            name = "Food",
            icon = "restaurant",
            color = "#E53935",
            type = "EXPENSE",
            monthlyBudgetInPaise = null,
            isDefault = false,
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )

        val id = db.categoryDao().insert(category)
        val retrieved = db.categoryDao().getById(id)

        assertNotNull(retrieved)
        assertEquals("Food", retrieved!!.name)
        assertEquals("EXPENSE", retrieved.type)
    }

    @Test
    fun getAllActive_excludesArchived() = runTest {
        val now = System.currentTimeMillis()

        db.categoryDao().insert(CategoryEntity(name = "Active", icon = null, color = null, type = "EXPENSE", monthlyBudgetInPaise = null, isDefault = false, isArchived = false, createdAt = now, updatedAt = now))
        val archivedId = db.categoryDao().insert(CategoryEntity(name = "Archived", icon = null, color = null, type = "EXPENSE", monthlyBudgetInPaise = null, isDefault = false, isArchived = false, createdAt = now, updatedAt = now))
        db.categoryDao().archive(archivedId, now)

        val active = db.categoryDao().getAllActive().first()
        assertTrue(active.none { it.name == "Archived" })
        assertTrue(active.any { it.name == "Active" })
    }

    @Test
    fun defaultCategories_cannotBeDeleted() = runTest {
        val now = System.currentTimeMillis()
        val id = db.categoryDao().insert(
            CategoryEntity(name = "Protected", icon = null, color = null, type = "EXPENSE", monthlyBudgetInPaise = null, isDefault = true, isArchived = false, createdAt = now, updatedAt = now)
        )

        db.categoryDao().delete(id)

        assertNotNull(db.categoryDao().getById(id))
    }
}
