package com.github.satilianius.bsonviewer.filetype

import com.github.satilianius.bsonviewer.icons.BsonIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class BsonFileType : FileType {
    // This is the intended way of using a singleton
    // https://plugins.jetbrains.com/docs/intellij/language-and-filetype.html
    @Suppress("CompanionObjectInExtension")
    companion object {
        @Suppress("unused") // `FileTypeManagerImpl` uses reflection to get this field
        val INSTANCE = BsonFileType()
        const val EXTENSION = "bson"
    }

    override fun getName(): String = "BSON"

    override fun getDescription(): String = "MongoDB BSON file"

    override fun getDefaultExtension(): String = EXTENSION

    override fun getIcon(): Icon? = BsonIcons.BsonIcon

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = false

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}
