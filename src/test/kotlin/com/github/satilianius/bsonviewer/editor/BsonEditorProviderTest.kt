package com.github.satilianius.bsonviewer.editor

import com.intellij.openapi.application.WriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.bson.BsonDocument
import org.bson.BsonBinaryWriter
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.EncoderContext
import org.bson.io.BasicOutputBuffer
import org.junit.Test

class BsonEditorProviderTest : BasePlatformTestCase() {

    /**
     * Helper method to convert a JSON string to BSON binary data
     */
    private fun jsonToBson(json: String): ByteArray {
        // Parse JSON to BsonDocument
        val bsonDocument = BsonDocument.parse(json)

        // Convert BsonDocument to BSON binary data
        val outputBuffer = BasicOutputBuffer()
        val writer = BsonBinaryWriter(outputBuffer)
        val codec = BsonDocumentCodec()
        codec.encode(writer, bsonDocument, EncoderContext.builder().build())

        return outputBuffer.toByteArray()
    }

    @Test
    fun testAccept() {
        val provider = BsonEditorProvider()

        // Create a BSON file
        val bsonContent = jsonToBson("{}")
        val bsonFile = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create a non-BSON file
        val textFile = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("test.txt", "text").virtualFile
        }

        // Test that provider accepts BSON files
        assertTrue("Provider should accept BSON files", provider.accept(project, bsonFile))

        // Test that provider rejects non-BSON files
        assertFalse("Provider should reject non-BSON files", provider.accept(project, textFile))
    }

    @Test
    fun testCreateEditor() {
        val provider = BsonEditorProvider()

        // Create a BSON file
        val bsonContent = jsonToBson("{}")
        val bsonFile = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create editor
        val editor = provider.createEditor(project, bsonFile)

        try {
            // Verify editor type
            assertTrue("Editor should be a BsonEditor", editor is BsonEditor)

            // Verify editor properties
            assertEquals("BSON Editor", editor.name)
            assertEquals(bsonFile, editor.file)
        } finally {
            // Clean up
            editor.dispose()
        }
    }

    @Test
    fun testEditorTypeId() {
        val provider = BsonEditorProvider()
        assertEquals("BsonEditor", provider.editorTypeId)
    }

    @Test
    fun testPolicy() {
        val provider = BsonEditorProvider()
        assertNotNull("Policy should not be null", provider.policy)
    }
}
