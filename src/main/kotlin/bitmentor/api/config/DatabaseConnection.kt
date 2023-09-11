package bitmentor.api.config

import bitmentor.api.config.GenericObjectMapper.getMapper
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.jackson2.Jackson2Config
import org.jdbi.v3.jackson2.Jackson2Plugin
import org.jdbi.v3.postgres.PostgresPlugin
import org.jdbi.v3.sqlobject.SqlObjectPlugin
import org.postgresql.ds.PGSimpleDataSource
import java.net.URI
import javax.sql.DataSource

object SharedJdbi {
    private val ds = getPGDataSource()
    private val sql = SqlObjectPlugin()

    private val postgresJdbi = getJdbi(ds)
        .installPlugin(sql)

    fun jdbi(): Jdbi {
        return postgresJdbi
    }

    fun hikariDs(): HikariDataSource {
        return ds
    }
}

private fun getJdbi(ds: HikariDataSource): Jdbi {
    val jdbi = Jdbi.create(ds)
        .installPlugin(PostgresPlugin())
        .installPlugin(KotlinPlugin())
        .installPlugin(Jackson2Plugin())

    jdbi.getConfig(Jackson2Config::class.java).mapper = getMapper()
    return jdbi
}

fun getPGDataSource(): HikariDataSource {
    val ds = PGSimpleDataSource()
    val dbUri = URI(Properties.postgresUrl)

    val username: String = dbUri.userInfo.split(":")[0]
    val password: String = dbUri.userInfo.split(":")[1]
    ds.setURL("jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.path)
    ds.currentSchema = Properties.postgres_schema
    ds.user = username
    ds.password = password
    ds.loadBalanceHosts = false

    return getHikariDataSource(
        getHikariConfig(
            ds
        )
    )
}

fun getHikariDataSource(config: HikariConfig): HikariDataSource = HikariDataSource(config)

fun getHikariConfig(ds: DataSource): HikariConfig {
    val hc = HikariConfig()

    hc.dataSource = ds
    hc.maximumPoolSize = Properties.maximum_pool_size
    hc.minimumIdle = Properties.minimum_idle_connections

    return hc
}
