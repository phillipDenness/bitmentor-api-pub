package bitmentor.api.config

import kotlinx.serialization.ImplicitReflectionSerializer
import nl.adaptivity.xmlutil.serialization.XML
import io.ktor.client.call.*
import io.ktor.client.features.json.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.*

@OptIn(ImplicitReflectionSerializer::class)
class XMLSerializer(private val format: XML = XML()) : JsonSerializer {

    override fun read(type: TypeInfo, body: Input): Any {
        val text = body.readText()
        val deserializationStrategy = format.context.getContextual(type.type)

        val mapper = deserializationStrategy
            ?: type.kotlinType?.let { serializer(it) }
            ?: type.type.serializer()

        return format.parse(mapper, text) ?: error("Failed to parse response of type $type. The result is null.")
    }

    override fun write(data: Any, contentType: ContentType): OutgoingContent {
        val serializer = data::class.serializer() as KSerializer<Any>
        val text = format.stringify(serializer, data, null)
        return TextContent(text, contentType)
    }
}
