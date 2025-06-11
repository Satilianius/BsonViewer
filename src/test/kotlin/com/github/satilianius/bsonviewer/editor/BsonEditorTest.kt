package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait

class BsonEditorTest : BasePlatformTestCase() {
    companion object {
        lateinit var editor: BsonEditor
    }
    // TODO the tests print error logs about improper disposing when ran in a batch, but not when ran individually
    fun testEditorProperties() {
        // language=JSON
        val bsonContent = jsonToBson("{}")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_properties.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        editor = BsonEditor(project, file)


        assertEquals("Editor name should be 'BSON Editor'", "BSON Editor", editor.name)
        assertEquals("Editor file should match the input file", file, editor.file)
        assertFalse("Editor should not be modified initially", editor.isModified)
        assertTrue("Editor should be valid", editor.isValid)
        assertNotNull("Editor component should not be null", editor.component)
        assertNotNull("Editor preferred focused component should not be null", editor.preferredFocusedComponent)

    }

    fun testEditorState() {
        // language=JSON
        val bsonContent = jsonToBson("""{"test": "value"}""")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_state.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        editor = BsonEditor(project, file)

        val state = editor.getState(FileEditorStateLevel.FULL)
        assertNotNull("Editor state should not be null", state)
    }

    fun testInvalidBsonEditorIsReadOnly() {
        val invalidContent = "This is not a BSON file".toByteArray()
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid_bson.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        editor = BsonEditor(project, file)

        // Verify that the editor is read-only
        assertTrue("Editor should be read-only for invalid BSON", editor.isViewer())
    }

    override fun tearDown() {
        // Give a chance for any pending async tasks to complete
        // before calling super.tearDown() which will dispose components
        runInEdtAndWait {
            editor.dispose()
        }
        super.tearDown()
    }
}
