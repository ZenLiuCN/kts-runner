import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import java.time.Duration

object Jooq {
    data class HikariConf(
        val driverClassName: String,
        val jdbcUrl: String,
        val username: String? = null,
        val password: String? = null,
        val connectionTimeout: Long = Duration.ofSeconds(30).toMillis(),
        val validationTimeout: Long = Duration.ofSeconds(5).toMillis(),
        val idleTimeout: Long = Duration.ofMinutes(10).toMillis(),
        val maxLifetime: Long = Duration.ofMinutes(30).toMillis(),
        val maxPoolSize: Int = 10,
        val minIdle: Int = -1,
        val isAutoCommit: Boolean = true,
        val isReadOnly: Boolean = false
    )

    enum class Dialect(val dialect: SQLDialect) {
        DEFAULT(SQLDialect.DEFAULT),
        CUBRID(SQLDialect.CUBRID),
        FIREBIRD(SQLDialect.FIREBIRD),
        FIREBIRD_2_5(SQLDialect.FIREBIRD_2_5),
        FIREBIRD_3_0(SQLDialect.FIREBIRD_3_0),
        H2(SQLDialect.H2),
        HSQLDB(SQLDialect.HSQLDB),
        MARIADB(SQLDialect.MARIADB),
        MYSQL(SQLDialect.MYSQL),
        MYSQL_5_7(SQLDialect.MYSQL_5_7),
        MYSQL_8_0(SQLDialect.MYSQL_8_0),
        POSTGRES(SQLDialect.POSTGRES),
        POSTGRES_9_3(SQLDialect.POSTGRES_9_3),
        POSTGRES_9_4(SQLDialect.POSTGRES_9_4),
        POSTGRES_9_5(SQLDialect.POSTGRES_9_5),
        POSTGRES_10(SQLDialect.POSTGRES_10),
        SQLITE(SQLDialect.SQLITE),
    }

    fun createDSL(dialect: SQLDialect, config: HikariConfig, settings: Settings) = run {
        val ds = HikariDataSource(config)
        DSL.using(ds, dialect, settings)
    }

    fun createDSL(dialect: Dialect, config: HikariConf, settings: Settings? = null) = run {
        val ds = HikariDataSource(HikariConfig().apply {
            driverClassName = config.driverClassName
            jdbcUrl = config.jdbcUrl
            connectionTimeout = config.connectionTimeout
            validationTimeout = config.validationTimeout
            idleTimeout = config.idleTimeout
            maxLifetime = config.maxLifetime
            maximumPoolSize = config.maxPoolSize
            minimumIdle = config.minIdle.let { if (it <= 0) config.maxPoolSize else it }
            isAutoCommit = config.isAutoCommit
            isReadOnly = config.isReadOnly
            config.username?.let { username = it }
            config.password?.let { password = it }
        })
        DSL.using(ds, dialect.dialect, settings)
    }
}
