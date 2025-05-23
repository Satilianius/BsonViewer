package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BsonEditorTest : BasePlatformTestCase() {

    // TODO this test prints error logs when with the testEditorState(), but not when run individually
    fun testEditorProperties() {
        // language=JSON
        val bsonContent = jsonToBson("{}")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_properties.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val editor = BsonEditor(project, file)

        try {
            assertEquals("Editor name should be 'BSON Editor'", "BSON Editor", editor.name)
            assertEquals("Editor file should match the input file", file, editor.file)
            assertFalse("Editor should not be modified initially", editor.isModified)
            assertTrue("Editor should be valid", editor.isValid)
            assertNotNull("Editor component should not be null", editor.component)
            assertNotNull("Editor preferred focused component should not be null", editor.preferredFocusedComponent)
        } finally {
            editor.dispose()
        }
    }

    fun testEditorState() {
        // language=JSON
        val bsonContent = jsonToBson("""{"test": "value"}""")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_state.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val editor = BsonEditor(project, file)

        try {
            val state = editor.getState(FileEditorStateLevel.FULL)
            assertNotNull("Editor state should not be null", state)

            editor.setState(state)
        } finally {
            editor.dispose()
        }
    }

    fun testInvalidBsonEditorIsReadOnly() {
        val invalidContent = "This is not a BSON file".toByteArray()
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid_bson.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        val editor = BsonEditor(project, file)

        try {
            // Verify that the editor is read-only
            assertTrue("Editor should be read-only for invalid BSON", editor.isViewer())
        } finally {
            editor.dispose()
        }
    }
}
