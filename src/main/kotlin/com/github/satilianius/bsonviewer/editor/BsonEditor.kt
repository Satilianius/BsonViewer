package com.github.satilianius.bsonviewer.editor

import com.intellij.json.JsonFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class BsonEditor(project: Project, private val virtualFile: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val bsonDocument = BsonDocument(virtualFile)
    private val jsonContent: String = bsonDocument.toJson()

    // Create a lightweight virtual file with a JSON file type
    val jsonFile = LightVirtualFile("${file.name}.json", JsonFileType.INSTANCE, jsonContent)

    // Creating an editor with the JSON virtual file should return JSON editor
    private val jsonEditor = TextEditorProvider.getInstance().createEditor(project, jsonFile) as TextEditor

    init {
        Disposer.register(this, jsonEditor)
        Disposer.register(this, bsonDocument)

        // Check if the BSON file content was parsed successfully
        bsonDocument.getErrorMessage()?.let { errorMessage ->
            // Disable editing for invalid BSON files to prevent overriding the original content
            if (jsonEditor.editor is EditorEx) {
                (jsonEditor.editor as EditorEx).isViewer = true
            }

            // Show the error dialog only if not in test mode
            if (!ApplicationManager.getApplication().isUnitTestMode) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        errorMessage,
                        "Error Loading BSON File"
                    )
                }
            }
        }

        // Add a document listener to convert JSON back to BSON on save
        jsonEditor.editor.document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    if (ApplicationManager.getApplication().isDispatchThread) {
                        val json = jsonEditor.editor.document.text
                        bsonDocument.setContent(json)
                        bsonDocument.save()
                    }
                }
            },
            jsonEditor
        )
    }

    fun isViewer(): Boolean = (jsonEditor.editor as? EditorEx)?.isViewer ?: false

    override fun getComponent(): JComponent = jsonEditor.component

    override fun getPreferredFocusedComponent(): JComponent? = jsonEditor.preferredFocusedComponent

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
        // The Disposer should handle disposal of the listener automatically
        // https://plugins.jetbrains.com/docs/intellij/disposers.html#registering-listeners-with-parent-disposable
         Disposer.dispose(jsonEditor)
    }
}
