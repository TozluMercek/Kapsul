package com.example.kapsul

import com.example.kapsul.data.Folder
import com.example.kapsul.ui.folders.flattenFolders
import org.junit.Assert.assertEquals
import org.junit.Test

class FoldersTest {
    @Test
    fun foldersFlattenDepthFirstWithDepths() {
        val folders = listOf(
            Folder(id = 1, name = "İş"),
            Folder(id = 2, name = "Ev"),
            Folder(id = 3, name = "Projeler", parentId = 1),
            Folder(id = 4, name = "Kapsül", parentId = 3),
            Folder(id = 5, name = "Alışveriş", parentId = 2),
        )
        val flat = flattenFolders(folders).map { (f, depth) -> f.name to depth }
        assertEquals(
            listOf("Ev" to 0, "Alışveriş" to 1, "İş" to 0, "Projeler" to 1, "Kapsül" to 2),
            flat,
        )
    }

    @Test
    fun orphanedFoldersStillShowAtTheTop() {
        val folders = listOf(Folder(id = 7, name = "Yetim", parentId = 99))
        assertEquals(listOf("Yetim" to 0), flattenFolders(folders).map { (f, d) -> f.name to d })
    }
}
