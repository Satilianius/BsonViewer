package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import de.undercouch.bson4jackson.BsonFactory
import org.bson.BsonDocument
import java.io.IOException

// IntelliJ wants the separators to be "\n" so it can do its own processing with it
// https://plugins.jetbrains.com/docs/intellij/modifying-psi.html?from=jetbrains.org#creating-the-new-psi
private const val IntelliJDefaultLineSeparator = "\n"
private val log = logger<BsonDocument>()

class BsonDocument(private val virtualFile: VirtualFile) : Disposable {
    companion object {
        private val JSON_MAPPER = ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY) // Used for validation
        private val BSON_MAPPER = ObjectMapper(BsonFactory())
    }

    private var jsonContent: String? = null
    private var isValidBson: Boolean = true
    private var errorMessage: String? = null

    init {
        loadBsonDocument()
    }

    private fun loadBsonDocument() {
        val content = virtualFile.contentsToByteArray()

        if (content.isEmpty()) {
            jsonContent = ""
            isValidBson = true // Empty file is technically valid
            errorMessage = null
            return
        }

        try {
            val jsonNode = BSON_MAPPER.readTree(content)

            jsonContent = JSON_MAPPER.writer(
                DefaultPrettyPrinter().withObjectIndenter(
                    DefaultIndenter().withLinefeed(IntelliJDefaultLineSeparator)))
                    .writeValueAsString(jsonNode)
            log.info("Successfully parsed BSON file content of {}".format(virtualFile.name))
            isValidBson = true
            errorMessage = null
        } catch (e: IOException) {
            log.info("Failed to read BSON file", e)
            jsonContent = ""
            isValidBson = false
            errorMessage = "File does not appear to be a valid BSON:\n%s".format(virtualFile.name)
        }
    }

    fun toJson(): String {
        return jsonContent?: ""
    }

    /**
     * Returns the error message if the BSON is invalid, null otherwise
     */
    fun getErrorMessage(): String? {
        return errorMessage
    }

    fun setContent(json: String) {
        this.jsonContent = json
        try {
            // Validate JSON by parsing it
            JSON_MAPPER.readTree(json)
            isValidBson = true
        } catch (e: JsonProcessingException) {
            log.debug("Invalid JSON format. Marking virtual file as invalid", e)
            // Keep the invalid JSON for editing, but mark as invalid
            isValidBson = false
        }
    }

    fun save() {
        try {
            // Only valid JSON can be converted to BSON, but since it is an expected state during editing,
            // just don't do anything and wait until the save call with a valid JSON
            if (!isValidBson) {
                log.debug("Not saving invalid BSON document to preserve previous valid content")
                return
            }

            jsonContent?.let {
                try {
                    if (it.isEmpty()) {
                        // Without explicitly setting binary content to an empty byte array,
                        // BSON mapper sets the content to be one byte (10), which is rendered as "null" in the editor
                        virtualFile.setBinaryContent(ByteArray(0))
                        return@let
                    }
                    // First, parse JSON string to JsonNode using a regular JSON mapper
                    val jsonNode = JSON_MAPPER.readTree(it)

                    // Convert to BSON bytes using BSON mapper
                    val bsonContent = BSON_MAPPER.writeValueAsBytes(jsonNode)

                    virtualFile.setBinaryContent(bsonContent)
                } catch (e: JsonProcessingException) {
                    log.error("Error converting JSON to BSON", e)
                    isValidBson = false
                }
            }
        } catch (e: Exception) {
            log.error("Error saving file", e)
        }
    }

    /**
     * Returns whether the file contains valid BSON data
     */
    fun isValidBson(): Boolean {
        return isValidBson
    }

    override fun dispose() {
        // Clear any references to potentially large objects
        jsonContent = null
    }
}
