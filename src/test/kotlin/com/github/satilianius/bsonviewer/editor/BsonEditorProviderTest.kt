package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.undercouch.bson4jackson.BsonFactory

class BsonEditorProviderTest : BasePlatformTestCase() {

    fun testAccept() {
        val provider = BsonEditorProvider()

        // language=json
        val bsonContent = jsonToBson("{\"test\":\"value\"}")
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val textFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            myFixture.addFileToProject("test.txt", "text").virtualFile
        }

        assertTrue("Provider should accept BSON files", provider.accept(project, bsonFile))
        assertFalse("Provider should reject non-BSON files", provider.accept(project, textFile))
    }

    fun testCreateEditor() {
        val provider = BsonEditorProvider()

        // language=json
        val bsonContent = jsonToBson("{\"test\":\"value\"}")
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val editor = provider.createEditor(project, bsonFile)

        try {
            assertInstanceOf(editor, BsonEditor::class.java)
            assertEquals(bsonFile, editor.file)
        } finally {
            editor.dispose()
        }
    }

    private fun jsonToBson(json: String): ByteArray {
        val jsonNode =  ObjectMapper().readTree(json)
        return ObjectMapper(BsonFactory()).writeValueAsBytes(jsonNode)
    }
}
