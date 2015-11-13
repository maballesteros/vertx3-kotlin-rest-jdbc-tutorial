import io.vertx.core.Vertx
import io.vertx.ext.web.Router

/**
 * Step01 - Start a simple web server
 */

object Vertx3KotlinRestJdbcTutorial {

    @JvmStatic fun main(args: Array<String>) {
        val vertx = Vertx.vertx()
        val server = vertx.createHttpServer()
        val port = 9000
        val router = Router.router(vertx)

        router.get("/").handler { it.response().end("Hello world!") }

        server.requestHandler { router.accept(it) }.listen(port) {
            if (it.succeeded()) println("Server listening at $port")
            else println(it.cause())
        }
    }
}