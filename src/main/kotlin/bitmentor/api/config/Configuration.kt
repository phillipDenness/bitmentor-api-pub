package bitmentor.api.config

import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import org.postgresql.util.PGobject

object Properties {
    const val JSON = "json"
    fun jsonObject() = PGobject()

    private val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromResource("application.properties")

    val server_port = config[Key("server.port", intType)]

    val postgresUrl = config[Key("database.url", stringType)]
    val postgres_schema = config[Key("postgres.schema", stringType)]
    val maximum_pool_size = config[Key("postgres.maximumPoolSize", intType)]
    val minimum_idle_connections = config[Key("postgres.minimumIdleConnections", intType)]


    val sendGridApiKey = config[Key("sendgrid.api.key", stringType)]

    val fromEmailAddress = config[Key("sendgrid.sender.email", stringType)]

    val jwtSecretActivation = config[Key("jwt.secret.activation", stringType)]
    val jwtSecretLogin = config[Key("jwt.secret.login", stringType)]
    val jwtSecretForgot = config[Key("jwt.secret.forgot", stringType)]

    val bbbUrl = config[Key("bbb.url", stringType)]
    val bbbSecret = config[Key("bbb.secret", stringType)]

    val emailActivateRedirectUrl = config[Key("email.activate.redirect.url", stringType)]
    val emailResetRedirectUrl = config[Key("email.reset.redirect.url", stringType)]
    val clientHostname = config[Key("client.url", stringType)]
    val clientProtocol = config[Key("client.protocol", stringType)]
    val clientUrl = "$clientProtocol://$clientHostname"

    val googleClientId = config[Key("google.client.id", stringType)]
    val githubClientId = config[Key("github.client.id", stringType)]
    val githubClientSecret = config[Key("github.client.secret", stringType)]
    val linkedinClientId = config[Key("linkedin.client.id", stringType)]
    val linkedinClientSecret = config[Key("linkedin.client.secret", stringType)]
    val notificationScheduleDelay = config[Key("notification.schedule.delay", longType)]

    val googleCredentialFile = config[Key("google.credential.file", stringType)]
    val environment = config[Key("environment", stringType)]

    val paymentFeePercent = config[Key("payment.fee", doubleType)]
    val squareUpMinPayment = config[Key("squareup.minPayment", doubleType)]

    val telegramApiToken = config[Key("telegram.apitoken", stringType)]
    val telegramChatId = config[Key("telegram.chatid", stringType)]

    val starlingToken = config[Key("starling.token", stringType)]
    val starlingUrl = config[Key("starling.url", stringType)]
    val starlingWebhookSecret = config[Key("starling.webhook.secret", stringType)]
    val reedApiToken = config[Key("reed.apitoken", stringType)]
    val reedUrl = config[Key("reed.url", stringType)]

    val paypalSandbox = config[Key("paypal.sandbox", booleanType)]
    val paypalClientId = config[Key("paypal.id", stringType)]
    val paypalSecret = config[Key("paypal.secret", stringType)]
    val paypalDisputeWebhook = config[Key("paypal.dispute.webhook", stringType)]
}
