package com.github.satilianius.bsonviewer.editor

import com.intellij.openapi.application.WriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test

class BsonDocumentTest : BasePlatformTestCase() {

    @Test
    fun testLoadValidJsonContent() {
        // Create a test file with valid JSON content
        val jsonContent = """{"name": "test", "value": 123}"""
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("test.bson", jsonContent).virtualFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Verify the JSON is correctly loaded
        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain '123'", json.contains("123"))
    }

    @Test
    fun testLoadInvalidContent() {
        // Create a test file with invalid content
        val invalidContent = "This is not JSON or BSON"
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("invalid.bson", invalidContent).virtualFile
        }

        // Create BsonDocument instance - should not throw exception
        val bsonDocument = BsonDocument(file)

        // Should return empty JSON object
        val json = bsonDocument.toJson()
        assertEquals("{}", json)
    }

    @Test
    fun testToJson() {
        // Create a test file with valid JSON content
        val jsonContent = """{"name": "test", "nested": {"key": "value"}}"""
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("test.bson", jsonContent).virtualFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Verify the JSON is correctly formatted
        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain 'nested'", json.contains("nested"))
        assertTrue("JSON should contain 'key'", json.contains("key"))
        assertTrue("JSON should contain 'value'", json.contains("value"))
    }

    @Test
    fun testFromJson() {
        // Create an empty test file
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("empty.bson", "").virtualFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Update with new JSON
        val newJson = """{"updated": true, "count": 42}"""
        bsonDocument.fromJson(newJson)

        // Verify the JSON was updated
        val json = bsonDocument.toJson()
        assertTrue("JSON should contain 'updated'", json.contains("updated"))
        assertTrue("JSON should contain 'true'", json.contains("true"))
        assertTrue("JSON should contain 'count'", json.contains("count"))
        assertTrue("JSON should contain '42'", json.contains("42"))
    }

    @Test
    fun testSave() {
        // Create a test file with initial content
        val initialJson = """{"initial": true}"""
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("save_test.bson", initialJson).virtualFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Update with new JSON
        val newJson = """{"updated": true, "saved": true}"""
        bsonDocument.fromJson(newJson)

        // Save the document using WriteAction
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        // Verify the file content was updated
        val updatedContent = WriteAction.computeAndWait<String, Throwable> {
            String(file.contentsToByteArray())
        }
        assertTrue("File content should contain 'updated'", updatedContent.contains("updated"))
        assertTrue("File content should contain 'saved'", updatedContent.contains("saved"))
    }
}
