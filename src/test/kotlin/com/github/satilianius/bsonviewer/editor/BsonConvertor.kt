package com.github.satilianius.bsonviewer.editor

import de.undercouch.bson4jackson.BsonFactory
import tools.jackson.databind.ObjectMapper

class BsonConvertor {
    companion object {
        fun jsonToBson(json: String): ByteArray {
            val jsonNode = ObjectMapper().readTree(json)
            return ObjectMapper(BsonFactory()).writeValueAsBytes(jsonNode)
        }
    }
}
