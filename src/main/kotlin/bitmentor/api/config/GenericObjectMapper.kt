package bitmentor.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule

object GenericObjectMapper {
    private val mapper = jacksonObjectMapper()
        .registerModule(Jdk8Module())
        .registerModule(JodaModule())
        .registerModule(JavaTimeModule())
        .registerModule(ParameterNamesModule())
        .registerModule(KotlinModule())

    fun getMapper(): ObjectMapper =
        mapper
}
