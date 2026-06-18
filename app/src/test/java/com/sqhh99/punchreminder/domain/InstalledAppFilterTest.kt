package com.sqhh99.punchreminder.domain

import com.sqhh99.punchreminder.domain.model.InstalledApp
import com.sqhh99.punchreminder.domain.usecase.InstalledAppFilter
import org.junit.Assert.assertEquals
import org.junit.Test

class InstalledAppFilterTest {

    private val filter = InstalledAppFilter()

    private val apps = listOf(
        InstalledApp("com.b", "Beta"),
        InstalledApp("com.a", "alpha"),
        InstalledApp("com.hidden", "Hidden", launchable = false),
        InstalledApp("com.a", "alpha-dup"),
        InstalledApp("com.self", "Self"),
    )

    @Test
    fun filtersUnlaunchable_dedupes_andSorts() {
        val result = filter.filter(apps)
        assertEquals(listOf("com.a", "com.b", "com.self"), result.map { it.packageName })
    }

    @Test
    fun excludesOwnPackage() {
        val result = filter.filter(apps, ownPackage = "com.self")
        assertEquals(listOf("com.a", "com.b"), result.map { it.packageName })
    }

    @Test
    fun queryMatchesLabelOrPackage_ignoreCase() {
        assertEquals(listOf("com.b"), filter.filter(apps, query = "beta").map { it.packageName })
        assertEquals(listOf("com.self"), filter.filter(apps, query = "com.self").map { it.packageName })
    }
}
