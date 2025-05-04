package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.filetype.BsonFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class BsonEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType is BsonFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return BsonEditor(project, file)
    }

    override fun getEditorTypeId(): String = "BsonEditor"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}
