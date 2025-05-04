package com.github.satilianius.bsonviewer.editor

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import org.bson.BsonDocument
import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings
import java.io.IOException

class BsonDocument(private val virtualFile: VirtualFile) {
    companion object {
        private val LOG = Logger.getInstance(BsonDocument::class.java)
        private val JSON_WRITER_SETTINGS = JsonWriterSettings.builder()
            .indent(true)
            .outputMode(JsonMode.EXTENDED)
            .build()
    }

    private var bsonDocument: BsonDocument? = null

    init {
        try {
            loadBsonDocument()
        } catch (e: Exception) {
            LOG.error("Failed to load BSON document", e)
        }
    }

    private fun loadBsonDocument() {
        try {
            val content = virtualFile.contentsToByteArray()
            if (content.isNotEmpty()) {
                try {
                    // Check if the file starts with a valid JSON character
                    val firstChar = content[0].toInt().toChar()
                    if (firstChar == '{' || firstChar == '[') {
                        // Likely JSON format
                        val jsonContent = String(content)
                        bsonDocument = BsonDocument.parse(jsonContent)
                    } else {
                        // Not JSON, create an empty document for now
                        // In a real implementation, we would parse binary BSON here
                        LOG.info("File does not appear to be in JSON format, creating empty document")
                        bsonDocument = BsonDocument()
                    }
                } catch (e: Exception) {
                    LOG.error("Failed to parse file content", e)
                    bsonDocument = BsonDocument()
                }
            } else {
                bsonDocument = BsonDocument()
            }
        } catch (e: IOException) {
            LOG.error("Error reading file", e)
            bsonDocument = BsonDocument()
        }
    }

    fun toJson(): String {
        return bsonDocument?.toJson(JSON_WRITER_SETTINGS) ?: "{}"
    }

    fun fromJson(json: String) {
        try {
            bsonDocument = BsonDocument.parse(json)
        } catch (e: Exception) {
            LOG.error("Failed to parse JSON", e)
        }
    }

    fun save() {
        try {
            bsonDocument?.let {
                val jsonContent = it.toJson(JSON_WRITER_SETTINGS)
                virtualFile.setBinaryContent(jsonContent.toByteArray())
            }
        } catch (e: IOException) {
            LOG.error("Error saving file", e)
        }
    }
}
