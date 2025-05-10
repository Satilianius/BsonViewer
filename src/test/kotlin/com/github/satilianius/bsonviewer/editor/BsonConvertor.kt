package com.github.satilianius.bsonviewer.editor

import com.fasterxml.jackson.databind.ObjectMapper
import de.undercouch.bson4jackson.BsonFactory

class BsonConvertor {
    companion object {
        fun jsonToBson(json: String): ByteArray {
            val jsonNode =  ObjectMapper().readTree(json)
            return ObjectMapper(BsonFactory()).writeValueAsBytes(jsonNode)
        }
    }
}
