package com.github.satilianius.bsonviewer.filetype

import com.intellij.openapi.application.WriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class BsonFileTypeTest : BasePlatformTestCase() {

    @Test
    fun testFileTypeProperties() {
        val fileType = BsonFileType.INSTANCE

        // Test basic properties
        assertEquals("BSON", fileType.name)
        assertEquals("MongoDB BSON file", fileType.description)
        assertEquals("bson", fileType.defaultExtension)

        // Test binary flag
        assertTrue("BSON file type should be binary", fileType.isBinary())

        // Test read-only flag
        assertFalse("BSON file type should not be read-only", fileType.isReadOnly())
    }

    @Test
    fun testFileTypeExtension() {
        val fileType = BsonFileType.INSTANCE
        assertEquals(BsonFileType.EXTENSION, fileType.defaultExtension)
        assertEquals("bson", BsonFileType.EXTENSION)
    }

    @Test
    fun testCharset() {
        val fileType = BsonFileType.INSTANCE
        // Create a test file
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("test.bson", "").virtualFile
        }
        // BSON is binary, so charset should be null
        assertNull("BSON file type charset should be null", fileType.getCharset(file, ByteArray(0)))
    }
}
