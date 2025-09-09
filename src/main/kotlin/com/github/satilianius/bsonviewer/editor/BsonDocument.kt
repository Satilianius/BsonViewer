package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import de.undercouch.bson4jackson.BsonFactory
import org.bson.BsonDocument
import java.io.ByteArrayOutputStream
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
    private var hasMultipleEntries: Boolean = false

    init {
        loadBsonDocument()
    }

    private fun loadBsonDocument() {
        val bsonInputStream = virtualFile.inputStream

        try {
            // Iterate BSON stream: one or many concatenated top-level documents
            val jsonNodes = mutableListOf<JsonNode>()
            bsonInputStream.use { input ->
                val reader = BSON_MAPPER.readerFor(JsonNode::class.java)
                val it = reader.readValues<JsonNode>(input)
                while (it.hasNextValue()) {
                    jsonNodes.add(it.nextValue())
                }
            }

            if (jsonNodes.isEmpty()) {
                jsonContent = ""
                isValidBson = true // Empty file is technically valid
                errorMessage = null
                hasMultipleEntries = false
                return
            }

            if (jsonNodes.size == 1) {
                // Single document: pretty-print JSON
                jsonContent = JSON_MAPPER.writer(
                    DefaultPrettyPrinter().withObjectIndenter(
                        DefaultIndenter().withLinefeed(IntelliJDefaultLineSeparator)
                    )
                ).writeValueAsString(jsonNodes[0])
                hasMultipleEntries = false
            } else {
                jsonContent = jsonNodes.joinToString(IntelliJDefaultLineSeparator) { JSON_MAPPER.writeValueAsString(it) }
                hasMultipleEntries = true
            }

            log.info("Successfully parsed BSON file content of ${virtualFile.name} (documents=${jsonNodes.size})")
            isValidBson = true
            errorMessage = null
        } catch (e: IOException) {
            log.info("Failed to read BSON file", e)
            jsonContent = ""
            isValidBson = false
            errorMessage = "File does not appear to be a valid BSON:\n%s".format(virtualFile.name)
            hasMultipleEntries = false
        }
    }

    fun toJson(): String {
        return jsonContent ?: ""
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
            val trimmed = json.trim()
            if (trimmed.isEmpty()) {
                isValidBson = true
                hasMultipleEntries = false
                return
            }

            val lines = json.split(IntelliJDefaultLineSeparator)
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val isJsonLines = isJsonLines(lines)

            if (isJsonLines) {
                // Every line parsed successfully as a standalone JSON value
                isValidBson = true
                hasMultipleEntries = true
                return
            }

            // Otherwise, validate as a single JSON value (can be pretty-printed multi-line)
            JSON_MAPPER.readTree(trimmed)
            isValidBson = true
            hasMultipleEntries = false
        } catch (e: JsonProcessingException) {
            log.debug("Invalid JSON format. Marking virtual file as invalid", e)
            isValidBson = false
            // Do not override hasMultipleEntries here; it will be recalculated on the next valid state
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

                    // Re-evaluate whether this is JSON lines or a single JSON value to be robust.
                    val lines = it.split(IntelliJDefaultLineSeparator)
                        .map { line -> line.trim() }
                        .filter { line -> line.isNotEmpty() }

                    if (isJsonLines(lines)) {
                        val out = ByteArrayOutputStream()
                        for (line in lines) {
                            val node = JSON_MAPPER.readTree(line)
                            val bytes = BSON_MAPPER.writeValueAsBytes(node)
                            out.write(bytes)
                        }
                        virtualFile.setBinaryContent(out.toByteArray())
                    } else {
                        // Single document (even if pretty-printed across multiple lines)
                        val jsonNode = JSON_MAPPER.readTree(it)
                        val bsonContent = BSON_MAPPER.writeValueAsBytes(jsonNode)
                        virtualFile.setBinaryContent(bsonContent)
                    }
                } catch (e: JsonProcessingException) {
                    log.error("Error converting JSON to BSON", e)
                    isValidBson = false
                }
            }
        } catch (e: Exception) {
            log.error("Error saving file", e)
        }
    }

    private fun isJsonLines(lines: List<String>) : Boolean {
        // If there are 2+ non-blank lines AND they are standalone JSON values, treat it as JSON lines (multi-entry).
        return lines.size > 1 && lines.all { line ->
            try {
                JSON_MAPPER.readTree(line)
                true
            } catch (_: JsonProcessingException) {
                false
            }
        }
    }

    /**
     * Returns whether the file contains valid BSON data
     */
    fun isValidBson(): Boolean {
        return isValidBson
    }

    fun hasMultipleEntries(): Boolean {
        return hasMultipleEntries
    }

    override fun dispose() {
        // Clear any references to potentially large objects
        jsonContent = null
    }
}
