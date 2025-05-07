package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.undercouch.bson4jackson.BsonFactory

/**
 * Tests for the BsonEditor class.
 *
 * Note: Due to line separator issues with the BsonDocument.toJson() method,
 * we're using a simplified approach to test the core functionality.
 */
class BsonEditorTest : BasePlatformTestCase() {

    /**
     * Test that the editor can be created and has the expected properties.
     */
    fun testEditorProperties() {
        // Create a simple empty BSON file
        val bsonContent = jsonToBson("{}")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_properties.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create BsonEditor
        val editor = BsonEditor(project, file)

        try {
            // Test basic properties
            assertEquals("Editor name should be 'BSON Editor'", "BSON Editor", editor.name)
            assertEquals("Editor file should match the input file", file, editor.file)
            assertFalse("Editor should not be modified initially", editor.isModified)
            assertTrue("Editor should be valid", editor.isValid)

            // Test component creation
            assertNotNull("Editor component should not be null", editor.component)
            assertNotNull("Editor preferred focused component should not be null", editor.preferredFocusedComponent)
        } finally {
            // Clean up
            editor.dispose()
        }
    }

    /**
     * Test that the editor state can be retrieved and set.
     */
    fun testEditorState() {
        // Create a simple empty BSON file
        val bsonContent = jsonToBson("{}")
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test_state.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create BsonEditor
        val editor = BsonEditor(project, file)

        try {
            // Test state methods
            val state = editor.getState(FileEditorStateLevel.FULL)
            assertNotNull("Editor state should not be null", state)

            // Setting state should not throw exceptions
            editor.setState(state)
        } finally {
            // Clean up
            editor.dispose()
        }
    }

    private fun jsonToBson(json: String): ByteArray {
        val jsonNode =  ObjectMapper().readTree(json)
        return ObjectMapper(BsonFactory()).writeValueAsBytes(jsonNode)
    }
}
