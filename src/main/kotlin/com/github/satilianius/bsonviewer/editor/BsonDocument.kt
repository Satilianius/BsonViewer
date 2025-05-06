package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import de.undercouch.bson4jackson.BsonFactory
import org.bson.BsonDocument
import java.io.IOException

class BsonDocument(private val virtualFile: VirtualFile) {
    companion object {
        private val LOG = Logger.getInstance(BsonDocument::class.java)
        private val JSON_MAPPER = ObjectMapper()
        private val BSON_MAPPER = ObjectMapper(BsonFactory())

        // Error message for invalid BSON format
        private const val INVALID_BSON_MESSAGE =
            "// This file does not appear to be in valid BSON format.\n" +
                    "// The original file content has been preserved.\n"
    }

    private var json: String? = null
    private var isValidBson: Boolean = true
    private var originalContent: ByteArray? = null

    init {
        loadBsonDocument()
    }

    private fun loadBsonDocument() {
        try {
            val content = virtualFile.contentsToByteArray()
            originalContent = content.copyOf() // Store original content

            if (content.isNotEmpty()) {
                try {
                    // Convert BSON bytes to JsonNode using BSON mapper
                    val jsonNode = BSON_MAPPER.readTree(content)
                    // Convert JsonNode to formatted JSON string using JSON mapper
                    json = JSON_MAPPER.writeValueAsString(jsonNode)
                    isValidBson = true
                } catch (e: Exception) {
                    // TODO: fix log for tests
//                    LOG.error("Failed to parse file content", e)
                    // For invalid BSON, show the error message instead of empty string
                    json = INVALID_BSON_MESSAGE
                    isValidBson = false
                }
            } else {
                json = "{}"
                isValidBson = true // Empty file is technically valid
            }
        } catch (e: IOException) {
            LOG.error("Error reading file", e)
            json = INVALID_BSON_MESSAGE
            isValidBson = false
        }
    }

    fun toJson(): String {
        return if (isValidBson) {
            json?: "{}"
        } else {
            INVALID_BSON_MESSAGE
        }
    }

    fun fromJson(json: String) {
        try {
            // Validate JSON by parsing it
            JSON_MAPPER.readTree(json)
            this.json = json
            isValidBson = true
        } catch (e: Exception) {
            LOG.error("Invalid JSON format", e)
            // Keep the invalid JSON for editing, but mark as invalid
            this.json = json
            isValidBson = false
        }
    }

    fun save() {
        try {
            if (isValidBson) {
                // Only save if the document is valid BSON
                json?.let {
                    try {
                        // First, parse JSON string to JsonNode using a regular JSON mapper
                        val jsonNode = JSON_MAPPER.readTree(it)

                        // Convert to BSON bytes using BSON mapper
                        val bsonContent = BSON_MAPPER.writeValueAsBytes(jsonNode)

                        virtualFile.setBinaryContent(bsonContent)
                    } catch (e: Exception) {
                        LOG.error("Error converting JSON to BSON", e)
                        isValidBson = false
                    }
                }
            } else {
                LOG.warn("Not saving invalid BSON document to preserve original content")
            }
        } catch (e: IOException) {
            LOG.error("Error saving file", e)
        }
    }

    /**
     * Returns whether the file contains valid BSON data
     */
    fun isValidBson(): Boolean {
        return isValidBson
    }
}
