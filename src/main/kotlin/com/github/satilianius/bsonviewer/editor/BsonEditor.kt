package com.github.satilianius.bsonviewer.editor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class BsonEditor(private val project: Project, private val virtualFile: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val bsonDocument = BsonDocument(virtualFile)
    private val jsonContent: String = bsonDocument.toJson()
    private val editorDocument: Document = EditorFactory.getInstance().createDocument(jsonContent)
    private val editor: Editor = EditorFactory.getInstance().createEditor(editorDocument, project, virtualFile, false)

    init {
        // Add document listener to save changes
        editorDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val json = editorDocument.text
                bsonDocument.fromJson(json)
                bsonDocument.save()
            }
        })
    }

    override fun getComponent(): JComponent = editor.component

    override fun getPreferredFocusedComponent(): JComponent? = editor.contentComponent

    override fun getName(): String = "BSON Editor"

    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

    override fun setState(state: FileEditorState) {
        // No state to restore
    }

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // Not needed for this implementation
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // Not needed for this implementation
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getFile(): VirtualFile = virtualFile

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editor)
    }
}
