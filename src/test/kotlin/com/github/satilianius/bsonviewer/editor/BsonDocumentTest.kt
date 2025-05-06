package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.undercouch.bson4jackson.BsonFactory
import org.bson.BsonDocument
import org.bson.BsonBinaryWriter
import org.bson.codecs.BsonDocumentCodec
import org.bson.codecs.EncoderContext
import org.bson.io.BasicOutputBuffer
import org.junit.Test

class BsonDocumentTest : BasePlatformTestCase() {

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
    fun testLoadValidJsonContent() {
        // Create a test file with valid BSON content
        // language=JSON
        val jsonContent = """{"name": "test", "value": 123}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Verify the JSON is correctly loaded
        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain '123'", json.contains("123"))

        // Verify the file is recognized as valid BSON
        assertTrue("File should be recognized as valid BSON", bsonDocument.isValidBson())
    }

    @Test
    fun testValidBsonFileNotOverwritten() {
        // Create a test file with valid BSON content
        val jsonContent = """{"original": true, "data": "should be preserved"}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("preserve_test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Verify it's valid BSON
        assertTrue("Should be valid BSON", bsonDocument.isValidBson())

        // Don't make any changes

        // Save the document
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        // Verify the file content still contains the original data
        val json = bsonDocument.toJson()
        assertTrue("JSON should contain 'original'", json.contains("original"))
        assertTrue("JSON should contain 'data'", json.contains("data"))
        assertTrue("JSON should contain 'should be preserved'", json.contains("should be preserved"))
    }

    @Test
    fun testLoadEmptyContent() {
        // Create an empty file
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            myFixture.addFileToProject("empty.bson", "").virtualFile
        }

        // Create BsonDocument instance
        val bsonDocument = BsonDocument(file)

        // Should return an empty JSON object for an empty file
        val json = bsonDocument.toJson()
        assertEquals("{}", json)

        // Empty file should be considered valid BSON
        assertTrue("Empty file should be valid BSON", bsonDocument.isValidBson())
    }

    @Test
    fun testInvalidBsonNotOverwritten() {
        // Create a file with completely invalid content (plain text)
        val invalidContent = "This is not a BSON file".toByteArray()

        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("invalid.bson", "").virtualFile
            vFile.setBinaryContent(invalidContent)
            vFile
        }

        // Create BsonDocument instance - this will throw an exception internally
        // but should still set isValidBson to false
        val bsonDocument = BsonDocument(file)

        // Try to save - should not overwrite the file because isValidBson is false
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

    @Test
    fun testToJson() {
        // Create a test file with valid BSON content
        val jsonContent = """{"name": "test", "nested": {"key": "value"}}"""
        val bsonContent = jsonToBson(jsonContent)
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
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
        // Create an empty BSON file
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("empty.bson", "").virtualFile
            // Initialize with an empty BSON document
            vFile.setBinaryContent(jsonToBson("{}"))
            vFile
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
        // Create a test file with initial BSON content
        val initialJson = """{"initial": true}"""
        val bsonContent = jsonToBson(initialJson)
        val file = WriteAction.computeAndWait<com.intellij.openapi.vfs.VirtualFile, Throwable> {
            val vFile = myFixture.addFileToProject("save_test.bson", "").virtualFile
            vFile.setBinaryContent(bsonContent)
            vFile
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

    @Test
    fun testConversionProducesSameResult() {
        // language=json
        val sourceJson = """{"name":"Chris","age":23,"address":{"city":"New York","country":"America"},"friends":[{"name":"Emily","hobbies":["biking","music","gaming"]},{"name":"John","hobbies":["soccer","gaming"]}]}"""


        // Create separate mappers for JSON and BSON
        val jsonMapper = ObjectMapper()
        val bsonMapper = ObjectMapper(BsonFactory())

        // First parse JSON string to JsonNode using a regular JSON mapper
        val jsonNode = jsonMapper.readTree(sourceJson)

        // Convert to BSON bytes using BSON mapper
        val bsonBytes = bsonMapper.writeValueAsBytes(jsonNode)

        // Convert BSON bytes back to JsonNode and then to string
        val convertedBack = bsonMapper.readTree(bsonBytes)
        val resultJson = jsonMapper.writeValueAsString(convertedBack)

        assertEquals(sourceJson, resultJson)
    }
}
