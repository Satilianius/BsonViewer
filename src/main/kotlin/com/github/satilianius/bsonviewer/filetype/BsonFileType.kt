package com.github.satilianius.bsonviewer.filetype

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class BsonFileType : FileType {
    companion object {
        val INSTANCE = BsonFileType()
        const val EXTENSION = "bson"
    }

    override fun getName(): String = "BSON"

    override fun getDescription(): String = "MongoDB BSON file"

    override fun getDefaultExtension(): String = EXTENSION

    override fun getIcon(): Icon? = null // TODO: Add icon

    override fun isBinary(): Boolean = true

    override fun isReadOnly(): Boolean = false

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null
}
