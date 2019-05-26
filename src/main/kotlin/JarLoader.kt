import org.slf4j.LoggerFactory
import java.beans.IntrospectionException
import java.io.File
import java.net.URLClassLoader
import java.lang.reflect.AccessibleObject.setAccessible
import java.net.URL


object JarLoader {
    private val log=LoggerFactory.getLogger(this::class.java)
    fun loadJar(jar: String) {
        val jarFile = File(jar).apply {
           try {
               if (!exists()) throw  Exception("${this.absoluteFile} is not exists")
               if (!isFile()) throw  Exception("${this.absoluteFile} is not file")
               if (!canRead()) throw  Exception("${this.absoluteFile} is not readable")
           }catch (e:Throwable){
               log.error(e.message)
               throw e
           }
        }
        addURLToSystemClassLoader(jarFile.toURI().toURL())
    }

    fun addURLToSystemClassLoader(url: URL) {
        val systemClassLoader = ClassLoader.getSystemClassLoader() as URLClassLoader
        val classLoaderClass = URLClassLoader::class.java
        try {
            val method = classLoaderClass.getDeclaredMethod("addURL", URL::class.java)
            method.isAccessible = true
            method.invoke(systemClassLoader, url)
        } catch (t: Throwable) {
            t.printStackTrace()
            throw IntrospectionException("Error when adding url to system ClassLoader ")
        }

    }
}
