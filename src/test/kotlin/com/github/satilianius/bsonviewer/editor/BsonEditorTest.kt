package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.openapi.util.Disposer

class BsonEditorTest : BasePlatformTestCase() {
    private var editor: BsonEditor? = null
    fun testEditorProperties() {
        // language=JSON
        val bsonContent = jsonToBson("{}")
        val bsonVirtualFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_properties.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        editor = BsonEditor(project, bsonVirtualFile)
        Disposer.register(testRootDisposable, editor!!)

        assertEquals("Editor name should be 'BSON Editor'", "BSON Editor", editor!!.name)
        assertEquals("Editor file should match the input file", bsonVirtualFile, editor!!.file)
        assertFalse("Editor should not be modified initially", editor!!.isModified)
        assertTrue("Editor should be valid", editor!!.isValid)
        assertNotNull("Editor component should not be null", editor!!.component)
        assertNotNull("Editor preferred focused component should not be null", editor!!.preferredFocusedComponent)
    }

    fun testInvalidBsonEditorIsReadOnly() {
        val invalidContent = "This is not a BSON file".toByteArray()
        val bsonVirtualFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid_bson.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        editor = BsonEditor(project, bsonVirtualFile)
        Disposer.register(testRootDisposable, editor!!)

        // Verify that the editor is read-only
        assertTrue("Editor should be read-only for invalid BSON", editor!!.isViewer())
    }

    fun testUndoActionWorks() {
        // language=JSON
        val bsonContent = jsonToBson("""{"name": "test", "value": 123}""")
        val bsonVirtualFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_undo.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }
        editor = BsonEditor(project, bsonVirtualFile)
        Disposer.register(testRootDisposable, editor!!)
        val initialText = editor!!.editor.document.text

        val psiFile = PsiManager.getInstance(project).findFile(editor!!.file)!!
        // Simulate user edit: change the JSON content
        runWriteCommandAction(project, "Edit JSON Text", null, { // language=JSON
            editor!!.editor.document.setText("""{ "changed": true }""")
        }, psiFile)
        assertTrue("Editor content should change after edit", initialText != editor!!.editor.document.text)

        // Now undo the change and verify the content reverts to the original JSON
        runInEdtAndWait {
            UndoManager.getInstance(project).undo(editor)
        }
        assertEquals("Undo should restore the previous JSON content", initialText, editor!!.editor.document.text)
    }

    override fun tearDown() {
        try {
            // Editor is already registered to testRootDisposable, no need to dispose manually
            editor = null
        } finally {
            super.tearDown()
        }
    }
}
