package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BsonEditorProviderTest : BasePlatformTestCase() {

    fun testAccept() {
        val provider = BsonEditorProvider()

        // language=JSON
        val bsonContent = jsonToBson("""{"test":451.0}""")
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

        // language=JSON
        val bsonContent = jsonToBson("""{"test":42}""")
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
}
