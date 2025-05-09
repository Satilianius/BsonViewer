package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import de.undercouch.bson4jackson.BsonFactory
import org.bson.BsonDocument
import java.io.IOException

// https://plugins.jetbrains.com/docs/intellij/modifying-psi.html?from=jetbrains.org#creating-the-new-psi
private const val IntelliJDefaultLineSeparator = "\n"

class BsonDocument(private val virtualFile: VirtualFile) {
    companion object {
        private val LOG = Logger.getInstance(BsonDocument::class.java)
        private val JSON_MAPPER = ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY) // Used for validation
        private val BSON_MAPPER = ObjectMapper(BsonFactory())

        // Error message for invalid BSON format
        private const val INVALID_BSON_MESSAGE =
            "// This file does not appear to be in valid BSON format.\n" +
                    "// The original file content has been preserved.\n"
    }

    private var jsonContent: String? = null
    private var isValidBson: Boolean = true
    private var originalContent: ByteArray? = null

    init {
        loadBsonDocument()
    }

    private fun loadBsonDocument() {
        try {
            val content = virtualFile.contentsToByteArray()
            originalContent = content.copyOf() // Store original content in case we need to restore it after an error

            if (content.isEmpty()) {
                jsonContent = ""
                isValidBson = true // Empty file is technically valid
                return
            }

            try {
                val jsonNode = BSON_MAPPER.readTree(content)

                jsonContent = JSON_MAPPER.writer(
                    DefaultPrettyPrinter().withObjectIndenter(
                        DefaultIndenter().withLinefeed(IntelliJDefaultLineSeparator)))
                        .writeValueAsString(jsonNode)
                isValidBson = true
            } catch (e: Exception) {
                // TODO: fix log for tests
                //                    LOG.error("Failed to parse file content", e)
                jsonContent = INVALID_BSON_MESSAGE
                isValidBson = false
            }
        } catch (e: IOException) {
            LOG.error("Error reading file", e)
            jsonContent = INVALID_BSON_MESSAGE
            isValidBson = false
        }
    }

    fun toJson(): String {
        return if (isValidBson) {
            jsonContent?: ""
        } else {
            INVALID_BSON_MESSAGE
        }
    }

    fun setContent(json: String) {
        this.jsonContent = json
        try {
            // Validate JSON by parsing it
            JSON_MAPPER.readTree(json)
            isValidBson = true
        } catch (e: JsonProcessingException) {
            LOG.debug("Invalid JSON format. Marking virtual file as invalid", e)
            // Keep the invalid JSON for editing, but mark as invalid
            isValidBson = false
        }
    }

    fun save() {
        try {
            // Only valid JSON can be converted to BSON, but since it is an expected state during editing,
            // just don't do anything and wait until the save call with a valid JSON
            if (!isValidBson) {
                LOG.debug("Not saving invalid BSON document to preserve original content")
                return
            }

            // TODO this may cause issues if the json content is fully removed. I would expect an empty bson file to be created
            jsonContent?.let {
                try {
                    // First, parse JSON string to JsonNode using a regular JSON mapper
                    val jsonNode = JSON_MAPPER.readTree(it)

                    // Convert to BSON bytes using BSON mapper
                    val bsonContent = BSON_MAPPER.writeValueAsBytes(jsonNode)

                    virtualFile.setBinaryContent(bsonContent)
                } catch (e: JsonProcessingException) {
                    LOG.error("Error converting JSON to BSON", e)
                    isValidBson = false
                }
            }
        } catch (e: Exception) {
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
