package com.github.satilianius.bsonviewer.editor

import com.github.satilianius.bsonviewer.editor.BsonConvertor.Companion.jsonToBson
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class BsonDocumentTest : BasePlatformTestCase() {

    fun testLoadValidBsonContent() {
        // language=JSON
        val jsonContent = """{"name": "test", "value": 123}"""
        val bsonContent = jsonToBson(jsonContent)
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("test.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain 'value'", json.contains("value"))
        assertTrue("JSON should contain '123'", json.contains("123"))

        assertTrue("File should be recognized as valid BSON", bsonDocument.isValidBson())
    }

    fun testLoadValidBsonDumpContent() {
        val bsonContent = javaClass.classLoader.getResourceAsStream("3tweets.bson")!!.readAllBytes()
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("3tweets.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        assertTrue("Should be valid BSON", bsonDocument.isValidBson())
        assertTrue("Should have multiple entries", bsonDocument.hasMultipleEntries())
        assertTrue("JSON content should not be empty", bsonDocument.toJson().isNotEmpty())
        assertEquals("JSON lines should contain expected number of elements",3, bsonDocument.toJson().split("\n").size)
    }

    fun testSavesValidMultilineJson() {
        val bsonContent = javaClass.classLoader.getResourceAsStream("3tweets.bson")!!.readAllBytes()
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("3tweets.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)
        //language=JSONLines
        bsonDocument.setContent("""{"line": "1", "value": 123}
{"line": "2", "value": 456}""")

        assertTrue("Should be valid BSON", bsonDocument.isValidBson())
        assertTrue("Should be Stream of BSONs", bsonDocument.hasMultipleEntries())
        assertTrue("JSON content should not be empty", bsonDocument.toJson().isNotEmpty())
        assertEquals("JSON lines should contain expected number of elements",2, bsonDocument.toJson().split("\n").size)

        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }
        assertFalse("JSON Lines should be updated", bsonFile.contentsToByteArray().contentEquals(bsonContent))
    }

    fun testOpenSingleBsonAndSaveAsMultiline() {
        // Start with a single-document BSON
        // language=JSON
        val single = """{"one": 1}"""
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("single_to_multi.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(jsonToBson(single))
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)
        assertTrue("Initial BSON should be valid", bsonDocument.isValidBson())
        assertFalse("Initial BSON should be single-entry", bsonDocument.hasMultipleEntries())

        // Turn it into JSON lines with two entries (duplicate or modified entries)
        // language=JSONLines
        val jsonLines = """{"one": 1}
{"two": 2}"""

        bsonDocument.setContent(jsonLines)
        WriteAction.runAndWait<Throwable> { bsonDocument.save() }

        // Reopen and verify multi-entry
        val reopened = BsonDocument(bsonFile)
        assertTrue("Reopened BSON should be valid", reopened.isValidBson())
        assertTrue("Reopened BSON should report multiple entries", reopened.hasMultipleEntries())

        val lines = reopened.toJson().split("\n")
        assertEquals("There should be exactly two JSON lines", 2, lines.size)
    }

    fun testOpenMultiEntryBsonAndSaveAsSingleDocument() {
        // Given a multi-entry BSON dump
        val bsonContent = javaClass.classLoader.getResourceAsStream("3tweets.bson")!!.readAllBytes()
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("multi_to_single.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)
        assertTrue("Initial BSON should be valid", bsonDocument.isValidBson())
        assertTrue("Initial BSON should be multi-entry", bsonDocument.hasMultipleEntries())

        // When a user sets a single valid JSON document and save, it should write a single BSON entry.
        // language=JSON
        val singleJson = """{"collapsed": true, "count": 1}"""
        bsonDocument.setContent(singleJson)
        WriteAction.runAndWait<Throwable> { bsonDocument.save() }

        // Then reopening should recognise it as a single entry
        val reopened = BsonDocument(bsonFile)
        assertTrue("Reopened BSON should be valid", reopened.isValidBson())
        assertFalse("Reopened BSON should be single-entry now", reopened.hasMultipleEntries())

        val json = reopened.toJson()
        assertTrue("JSON should contain 'collapsed'", json.contains("collapsed"))
        assertTrue("JSON should contain 'count'", json.contains("count"))
    }

    fun testValidBsonFileNotOverwritten() {
        // language=JSON
        val jsonContent = """{"original": true, "data": "should be preserved"}"""
        val bsonContent = jsonToBson(jsonContent)
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("preserve_test.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

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
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            myFixture.addFileToProject("empty.bson", "").virtualFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        val json = bsonDocument.toJson()
        assertEquals("", json)
        assertTrue("Empty file should be valid BSON", bsonDocument.isValidBson())
    }

    fun testInvalidBsonNotOverwritten() {
        val invalidContent = "This is not a BSON file".toByteArray()

        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("invalid.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(invalidContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        // Try to save - should not overwrite the file
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        // Verify the file content was not changed
        val updatedContent = WriteAction.computeAndWait<ByteArray, Throwable> {
            bsonFile.contentsToByteArray()
        }

        // Content should still be the original invalid content
        assertOrderedEquals(invalidContent, updatedContent)
    }

    fun testToJson() {
        // language=JSON
        val jsonContent = """{"name": "test", "nested": {"key": "value"}}"""
        val bsonContent = jsonToBson(jsonContent)
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("test.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        val json = bsonDocument.toJson()
        // The format might be different, so we check for the presence of the key-value pairs
        assertTrue("JSON should contain 'name'", json.contains("name"))
        assertTrue("JSON should contain 'test'", json.contains("test"))
        assertTrue("JSON should contain 'nested'", json.contains("nested"))
        assertTrue("JSON should contain 'key'", json.contains("key"))
        assertTrue("JSON should contain 'value'", json.contains("value"))
    }

    fun testSetContent() {
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("empty.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(jsonToBson("{}"))
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

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
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("save_test.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        // language=JSON
        val newJson = """{"updated": true, "saved": true}"""
        bsonDocument.setContent(newJson)

        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }

        val updatedContent = WriteAction.computeAndWait<String, Throwable> {
            String(bsonFile.contentsToByteArray())
        }
        assertTrue("File content should contain 'updated'", updatedContent.contains("updated"))
        assertTrue("File content should contain 'saved'", updatedContent.contains("saved"))
    }

    fun testConversionProducesSameResult() {
        val bsonContent = javaClass.classLoader.getResourceAsStream("exampleGlossary.bson")!!.readAllBytes()
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("save_test.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(bsonContent)
            virtualBsonFile
        }
        val expectedJson = javaClass.classLoader.getResourceAsStream("exampleGlossary.json")!!.readAllBytes().decodeToString()

        val bsonDocument = BsonDocument(bsonFile)

        assertSameLines(expectedJson, bsonDocument.toJson())
    }

    fun testSavingEmptyStringSavesEmptyBson() {
        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            myFixture.addFileToProject("empty_string.bson", "").virtualFile
        }
        val bsonDocument = BsonDocument(bsonFile)
        bsonDocument.setContent("")
        WriteAction.runAndWait<Throwable> {
            bsonDocument.save()
        }
        val updatedContent = WriteAction.computeAndWait<ByteArray, Throwable> {
            bsonFile.contentsToByteArray()
        }
        assertEquals(0, updatedContent.size)
    }

    fun testInvalidBsonSetsErrorMessage() {
        val invalidContent = "This is not a BSON file".toByteArray()

        val bsonFile = WriteAction.computeAndWait<VirtualFile, Throwable> {
            val virtualBsonFile = myFixture.addFileToProject("invalid_with_error.bson", "").virtualFile
            virtualBsonFile.setBinaryContent(invalidContent)
            virtualBsonFile
        }

        val bsonDocument = BsonDocument(bsonFile)

        // Verify that the document is marked as invalid
        assertFalse("File should be recognized as invalid BSON", bsonDocument.isValidBson())

        // Verify that the error message is set
        assertNotNull("Error message should not be null", bsonDocument.getErrorMessage())

        // Verify that toJson returns an empty string for invalid BSON
        assertEquals("toJson should return empty string for invalid BSON", "", bsonDocument.toJson())
    }
}
