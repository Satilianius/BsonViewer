package com.github.satilianius.bsonviewer.editor

import com.intellij.json.JsonFileType
import com.intellij.json.jsonLines.JsonLinesFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.BulkAwareDocumentListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
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
import com.intellij.pom.Navigatable
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.application
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class BsonEditor(project: Project, private val virtualFile: VirtualFile) : UserDataHolderBase(), TextEditor {
    private val log = Logger.getInstance(BsonEditor::class.java)
    private val bsonDocument = BsonDocument(virtualFile)
    private val jsonContent: String = bsonDocument.toJson()

    // Create a lightweight virtual file with a JSON file type
    val jsonVirtualFile =
        if (bsonDocument.hasMultipleEntries())
            LightVirtualFile("${virtualFile.name}.ndjson", JsonLinesFileType.INSTANCE, jsonContent)
        else LightVirtualFile("${virtualFile.name}.json", JsonFileType.INSTANCE, jsonContent)

    // Creating an editor with the JSON virtual file should return JSON editor
    private val jsonEditor = TextEditorProvider.getInstance().createEditor(project, jsonVirtualFile) as TextEditor

    private val messageBusConnection = application.messageBus.connect(this)
    @Volatile private var jsonDocumentChanged = false

    init {
        log.info("Initializing BsonEditor")
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

        // Mark as dirty when JSON text changes
        jsonEditor.editor.document.addDocumentListener(object : BulkAwareDocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                jsonDocumentChanged = true
            }
        }, this)

        messageBusConnection.subscribe(
            FileDocumentManagerListener.TOPIC,
            object: FileDocumentManagerListener {
                private val logger = Logger.getInstance(BsonEditor::class.java)
                override fun beforeDocumentSaving(document: Document) {
                    logger.info("beforeDocumentSaving $document")
                    super.beforeDocumentSaving(document)
                }

                override fun beforeAnyDocumentSaving(document: Document, explicit: Boolean) {
                    logger.info("beforeAnyDocumentSaving $document")
                    super.beforeAnyDocumentSaving(document, explicit)
                }
                override fun beforeAllDocumentsSaving() {
                    logger.info("beforeAllDocumentsSaving. dirty=$jsonDocumentChanged, isModified=${isModified()}")
                    if (!jsonDocumentChanged) {
                        return
                    }
                    val doc = FileDocumentManager.getInstance().getDocument(jsonVirtualFile)
                    val textToSave = doc?.text ?: bsonDocument.toJson()
                    if (textToSave.isEmpty() && bsonDocument.toJson().isEmpty()) return

                    logger.info("Saving JSON view for ${jsonVirtualFile.name} (dirty=$jsonDocumentChanged)")
                    CommandProcessor.getInstance().runUndoTransparentAction {
                        ApplicationManager.getApplication().runWriteAction {
                            bsonDocument.setContent(textToSave)
                            bsonDocument.save()
                            jsonDocumentChanged = false
                        }
                    }
                }
            }
        )

        // Add a document listener to convert JSON back to BSON on document change
//        jsonEditor.editor.document.addDocumentListener(
//            object : DocumentListener {
//                override fun documentChanged(event: DocumentEvent) {
//                    if (ApplicationManager.getApplication().isDispatchThread) {
//                        val json = jsonEditor.editor.document.text
//                        bsonDocument.setContent(json)
//                        bsonDocument.save()
//                    }
//                }
//            },
//            jsonEditor
//        )
    }

    fun isViewer(): Boolean = (jsonEditor.editor as? EditorEx)?.isViewer ?: false

    override fun getComponent(): JComponent = jsonEditor.component

    override fun getPreferredFocusedComponent(): JComponent? = jsonEditor.preferredFocusedComponent

    override fun getName(): String = "BSON Editor"

    override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

    override fun setState(state: FileEditorState) {
        // No state to restore
    }

    override fun isModified(): Boolean = this.jsonEditor.isModified

    override fun isValid(): Boolean = true

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        // Not needed for this implementation
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        // Not needed for this implementation
    }

    override fun getCurrentLocation(): FileEditorLocation? = null

    override fun getFile(): VirtualFile = virtualFile

    override fun getEditor(): Editor {
        return jsonEditor.editor
    }

    override fun canNavigateTo(navigatable: Navigatable): Boolean {
        return jsonEditor.canNavigateTo(navigatable)
    }

    override fun navigateTo(navigatable: Navigatable) {
        jsonEditor.navigateTo(navigatable)
    }

    override fun dispose() {
        // The Disposer should handle disposal of the listener automatically
        // https://plugins.jetbrains.com/docs/intellij/disposers.html#registering-listeners-with-parent-disposable
        Disposer.dispose(jsonEditor)
    }
}
