package com.android.purebilibili.feature.list

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PersonalListSelectorStructureTest {

    @Test
    fun favoriteCategoryChips_areRemovedFromHeader() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
        )

        assertFalse(source.contains("FavoriteSection.entries.forEach"))
        assertFalse(source.contains("LazyRow("))
        assertFalse(source.contains("systemGestureExclusion"))
    }

    @Test
    fun favoriteFolderNavigation_usesSelectorAndProgrammaticPagerOnly() {
        val source = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/CommonListScreen.kt",
        )
        val pagerSection = source
            .substringAfter("} else when (favoriteContentMode) {")
            .substringAfter("FavoriteContentMode.PAGER ->")
            .substringBefore("FavoriteContentMode.SINGLE_FOLDER ->")

        assertTrue(source.contains("FavoriteFolderSelector("))
        assertTrue(source.contains("AppDropdownMenu("))
        assertFalse(source.contains("FavoriteFolderSummary("))
        assertFalse(source.contains("selectedValue = favoriteBrowseSection"))
        assertTrue(pagerSection.contains("userScrollEnabled = false"))
        assertFalse(pagerSection.contains("verticalPriorityHorizontalPagerSwipe"))
    }

    @Test
    fun secondaryPersonalFilters_doNotRequireHorizontalDrag() {
        val categorySource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/list/FavoriteCategoryScreen.kt",
        )
        val watchLaterSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt",
        )
        val profileSource = loadSource(
            "app/src/main/java/com/android/purebilibili/feature/profile/ProfileScreen.kt",
        )

        assertTrue(categorySource.contains("FlowRow("))
        assertFalse(categorySource.contains("LazyRow("))
        assertFalse(watchLaterSource.contains("WatchLaterFilter.entries.forEach"))
        assertFalse(watchLaterSource.contains("LazyRow("))
        val profileTabs = profileSource
            .substringAfter("private fun ProfileSpaceTabs(")
            .substringBefore("private fun ProfileSpaceTabBody(")
        assertTrue(profileTabs.contains("dragSelectionEnabled = false"))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath),
            File("app/$normalizedPath"),
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
