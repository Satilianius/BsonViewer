package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BsonDocumentTest : BasePlatformTestCase() {

    fun testLoadValidJsonContent() {
        // language=JSON
        val jsonContent = """{"name": "test", "value": 123}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain 'value'", json.contains("value"))
        assertTrue("JSON should contain '123'", json.contains("123"))

        assertTrue("File should be recognized as valid BSON", bsonDocument.isValidBson())
    }

    fun testValidBsonFileNotOverwritten() {
        // language=JSON
        val jsonContent = """{"original": true, "data": "should be preserved"}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("preserve_test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        assertTrue("Should be valid BSON", bsonDocument.isValidBson())

        // Don't make any changes and try to save the document
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        // Verify the file content still contains the original data
        val json = bsonDocument.toJson()
        assertTrue("JSON should contain 'original'", json.contains("original"))
        assertTrue("JSON should contain 'data'", json.contains("data"))
        assertTrue("JSON should contain 'should be preserved'", json.contains("should be preserved"))
    }

    fun testLoadEmptyContent() {
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            myFixture.addFileToProject("empty.bson", "").virtualFile
        }

        val bsonDocument = BsonDocument(file)

        val json = bsonDocument.toJson()
        assertEquals("", json)
        assertTrue("Empty file should be valid BSON", bsonDocument.isValidBson())
    }

    fun testInvalidBsonNotOverwritten() {
        val invalidContent = "This is not a BSON file".toByteArray()

        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        // Try to save - should not overwrite the file
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        // Verify the file content was not changed
        val updatedContent = WriteAction.computeAndWait<ByteArray, Throwable> {
            file.contentsToByteArray()
        }

        // Content should still be the original invalid content
        assertOrderedEquals(invalidContent, updatedContent)
    }

    fun testToJson() {
        // language=JSON
        val jsonContent = """{"name": "test", "nested": {"key": "value"}}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain 'nested'", json.contains("nested"))
        assertTrue("JSON should contain 'key'", json.contains("key"))
        assertTrue("JSON should contain 'value'", json.contains("value"))
    }

    fun testSetContent() {
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("empty.bson", "").virtualFile
            vFile.setBinaryContent(jsonToBson("{}"))
            vFile
        }

        val bsonDocument = BsonDocument(file)

        // language=JSON
        val newJson = """{"updated": true, "count": 42}"""
        bsonDocument.setContent(newJson)

        val json = bsonDocument.toJson()
        assertTrue("JSON should contain 'updated'", json.contains("updated"))
        assertTrue("JSON should contain 'true'", json.contains("true"))
        assertTrue("JSON should contain 'count'", json.contains("count"))
        assertTrue("JSON should contain '42'", json.contains("42"))
    }

    fun testSave() {
        // language=JSON
        val initialJson = """{"initial": true}"""
        val bsonContent = jsonToBson(initialJson)
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("save_test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        // language=JSON
        val newJson = """{"updated": true, "saved": true}"""
        bsonDocument.setContent(newJson)

        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        val updatedContent = WriteAction.computeAndWait<String, Throwable> {
            String(file.contentsToByteArray())
        }
        assertTrue("File content should contain 'updated'", updatedContent.contains("updated"))
        assertTrue("File content should contain 'saved'", updatedContent.contains("saved"))
    }

    fun testConversionProducesSameResult() {
        val bsonContent = javaClass.classLoader.getResourceAsStream("exampleGlossary.bson")!!.readAllBytes()
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("save_test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }
        val expectedJson = javaClass.classLoader.getResourceAsStream("exampleGlossary.json")!!.readAllBytes().decodeToString()

        val bsonDocument = BsonDocument(file)

        assertSameLines(expectedJson, bsonDocument.toJson())
    }

    fun testSavingEmptyStringSavesEmptyBson() {
        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            myFixture.addFileToProject("empty_string.bson", "").virtualFile
        }
        val bsonDocument = BsonDocument(file)
        bsonDocument.setContent("")
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }
        val updatedContent = WriteAction.computeAndWait<ByteArray, Throwable> {
            file.contentsToByteArray()
        }
        assertEquals(0, updatedContent.size)
    }

    fun testInvalidBsonSetsErrorMessage() {
        val invalidContent = "This is not a BSON file".toByteArray()

        val file = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid_with_error.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        val bsonDocument = BsonDocument(file)

        // Verify that the document is marked as invalid
        assertFalse("File should be recognized as invalid BSON", bsonDocument.isValidBson())

        // Verify that the error message is set
        assertNotNull("Error message should not be null", bsonDocument.getErrorMessage())

        // Verify that toJson returns an empty string for invalid BSON
        assertEquals("toJson should return empty string for invalid BSON", "", bsonDocument.toJson())
    }
}
