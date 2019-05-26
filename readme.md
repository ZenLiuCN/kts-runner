# jar for run a kts (kotlin script) simply

1. contains JOOQ with HikariCP for easy use SQL
2. contains jar loader
3. contains Logger (Logback)
## partly surpport org.jetbrains.kotlin.script.util Annotations

1. @file:DependsOn("org.xerial:sqlite-jdbc:3.27.2.1")

    add dependency
2. @file:Repository("https://maven.aliyun.com/repository/central")

    add customer maven repository

3. @file:Import("some.kts")

    import other script

## useage
my.kts
```kotlin
@file:DependsOn("org.xerial:sqlite-jdbc:3.27.2.1")
import org.jetbrains.kotlin.script.util.DependsOn

val log=getLogger("Script")
val dsl = Jooq.createDSL(
    Jooq.Dialect.SQLITE,
    Jooq.HikariConf(
        "org.sqlite.JDBC",
        "jdbc:sqlite::memory:"
    )
)
//language=SQLite
dsl.execute(
    """
    CREATE TABLE IF NOT EXISTS TEST(
      id INTEGER PRIMARY KEY AUTOINCREMENT ,
      name TEXT NOT NULL
    )
""".trimIndent()
).let{"$it"}.let(log::info)
dsl.execute(
    """
    INSERT INTO TEST (name) VALUES ('aaa')
""".trimIndent()
).let{"$it"}.let(log::info)
dsl.resultQuery("""SELECT * FROM TEST""").fetch().intoMaps().let{"$it"}.let(log::info)

```
exectue with
```shell
java -jar kts-runner.jar my.kts
```

then will get
```
2019-05-26 21:22:42.260 INFO  [main] Maven (Maven.kt:89) - jars loaded [org.xerial-sqlite-jdbc-3.27.2.1.jar]
2019-05-26 21:22:42.260 INFO  [main] ktsRunner (main.kt:80) - process script /home/user/my.kts
2019-05-26 21:22:45.411 INFO  [main] com.zaxxer.hikari.HikariDataSource (HikariDataSource.java:80) - HikariPool-1 - Starting...
2019-05-26 21:22:45.474 INFO  [main] com.zaxxer.hikari.HikariDataSource (HikariDataSource.java:82) - HikariPool-1 - Start completed.
2019-05-26 21:22:45.645 INFO  [main] Script (Line_1.kts:21) - 0
2019-05-26 21:22:45.645 INFO  [main] Script (Line_1.kts:26) - 1
2019-05-26 21:22:45.709 INFO  [main] Script (Line_1.kts:27) - [{id=1, name=aaa}]
```
