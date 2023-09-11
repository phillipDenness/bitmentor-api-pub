package bitmentor.api.service

import bitmentor.api.config.Properties
import bitmentor.api.config.getPGDataSource
import kotlin.system.exitProcess
import mu.KotlinLogging
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

fun doMigrate() {
    try {
        getConnection().migrate()
    } catch (e: FlywayException) {
        logger.error { "Exception during flyway migration $e. Shutting down JVM." }
        shutdown()
    }
}

fun getConnection(): Flyway {
    return Flyway.configure().dataSource(getPGDataSource()).schemas("public").defaultSchema("public").load()
}

private fun shutdown() {
    exitProcess(1)
}
