import okhttp3.*
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.time.Duration

object Maven {
    private val log = LoggerFactory.getLogger(this::class.java)
    private fun client() = OkHttpClient.Builder().apply {
        connectTimeout(Duration.ofSeconds(5))
        followRedirects(true)
        followSslRedirects(true)
    }.build()

    private var local = File(".mvn").apply {
        if (!exists()) mkdirs()
        if (isFile) throw Exception(".mvn already exists as a file !")
    }

    internal fun OkHttpClient.asyncGet(url: String, cb: (res: Response?, error: Exception?) -> Any) {
        this.newCall(
            Request.Builder()
                .get()
                .url(url)
                .build()
        ).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cb.invoke(null, e)
            }

            override fun onResponse(call: Call, response: Response) {
                cb.invoke(response, null)
            }
        })
    }

    internal fun OkHttpClient.get(url: String) = run {
        this.newCall(
            Request.Builder()
                .get()
                .url(url)
                .build()
        ).execute()
    }

    internal fun OkHttpClient.head(url: String) = run {
        this.newCall(
            Request.Builder()
                .head()
                .url(url)
                .build()
        ).execute()
    }

    private val repo = mutableListOf<String>(
        "http://central.maven.org/maven2"
    )

    fun addRepository(url: String) {
        repo.add(0, url)
    }

    fun fetchJars(vararg jar: String) {
        jar.map {
            fetch(it)
        }.flatten().parallelStream().map {
            log.info("try fetch  $it ")
            val repo = testRepo(it)
            fetchJar(repo, it)
        }.count().let {
            log.info("download $it of jars")
        }
    }

    fun fetchAndLoadJars(vararg jar: String) {
        jar.map {
            fetch(it)
        }.flatten().parallelStream().map {
            log.info("try fetch  $it ")
            val repo = testRepo(it)
            fetchJar(repo, it)
        }.count().let {
            //            log.info("download $it of jars")
        }
        local.walk().filter { it.isFile && it.extension == "jar" }.map {
            JarLoader.loadJar(it.absolutePath)
            it.absoluteFile.name
        }.toList().let {
            log.info("jars loaded $it")
        }

    }

    fun loadJars() = local.walk().filter { it.isFile && it.extension == "jar" }.map {
        JarLoader.loadJar(it.absolutePath)
        it.absoluteFile.name
    }.toList().let {
        log.info("jars loaded $it")
    }

    private fun fetch(jar: String) = run {
        val repo = testRepo(jar)
        fetchPom(repo, jar).toMutableSet().apply {
            add(jar)
        }.filterNot {
            it.toJar().exists() && it.toJar().isFile
        }
    }

    private fun fetchJar(raw: String, jar: String) = run {
        log.info("fetching $jar from $raw")
        client().get(pom(raw, jar).replace(".pom", ".jar")).body()!!.byteStream().copyTo(jar.toJar().outputStream())
            .let {
                log.info("fetched $jar($it) from $raw")
                it
            }
    }


    private fun String.toJar() = run {
        val (g, a, v) = split(":")
        File(local, "$g-$a-$v.jar")
    }

    private fun testRepo(jar: String) = run {
        var res = ""
        client().let out@{ client ->
            repo.forEach {
                if (client.head(pom(it, jar)).isSuccessful) {
                    res = it
                    return@out
                }
            }
        }
        res
    }

    private fun pom(raw: String, art: String) = run {
        val (g, a, v) = art.split(":")
        "$raw/${g.replace(".", "/")}/$a/$v/$a-$v.pom"
    }

    private fun parsePom(pom: String) = try {
        parseXml(pom).el("project")!!.let {
            it.el("dependencies")!!.childNodes!!.map {
                if (it.nodeName == "dependency" && it.el("scope")?.text != "provided" && it.el("scope")?.text != "test") {
                    "${it.el("groupId").text}:${it.el("artifactId").text}:${it.el("version")?.text?.let { "$it" }
                        ?: ""}"
                } else null
            }.filterNotNull() to it.el("parent")?.let {
                "${it.el("groupId").text}:${it.el("artifactId").text}:${it.el("version").text}"
            }
        }
    } catch (e: Exception) {
        throw Throwable("invalid artifact pom ", e)
    }

    private fun parseParent(ppom: String) = try {
        parseXml(ppom).el("project")!!.let { prj ->
            (prj.el("dependencyManagement")!!.el("dependencies")!!.childNodes!!.map {
                if (it.nodeName == "dependency" && it.el("scope")?.text != "provided" && it.el("scope")?.text != "test") {
                    "${it.el("groupId").text}:${it.el("artifactId").text}${it.el("version")?.text?.let { ":$it" }
                        ?: ""}"
                } else null
            }.filterNotNull() to prj.el("parent")?.let {
                "${it.el("groupId").text}:${it.el("artifactId").text}:${it.el("version").text}"
            }).let { (dep, parent) ->
                dep.map {
                    if (it.contains("\$")) {
                        val (g, a, v) = it.split(":")
                        "$g:$a:${prj.el("properties")!!.el(v.removePrefix("\${").removeSuffix("}"))!!.text}"
                    } else it
                } to parent
            }
        }
    } catch (e: Exception) {
        throw Throwable("invalid parent pom ", e)
    }

    private fun fetchPom(raw: String, art: String) = run {
        parsePom(client().get(pom(raw, art)).body()!!.string()).let { (dep, pat) ->
            if (dep.find { it.endsWith(":") } != null && pat != null) {
                fetchParent(raw, dep, pat)
            } else dep to pat
        }.let { (dep, _) ->
            dep
        }
    }

    private fun fetchParent(raw: String, dep: List<String>, pat: String): Pair<List<String>, String?> = run {
        parseParent(client().get(pom(raw, pat)).body()!!.string()).let { (pro, pats) ->
            dep.map { d ->
                if (d.endsWith(":")) {
                    pro.find { it.startsWith(d) } ?: d
                } else d
            }.let {
                if (it.find { it.endsWith(":") } != null && pats != null) {
                    fetchParent(raw, it, pats)
                } else {
                    it to pat
                }
            }
        }
    }
}



