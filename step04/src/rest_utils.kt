import com.google.gson.Gson
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import kotlin.reflect.KClass

/**
 * Created by mike on 14/11/15.
 */

val GSON = Gson()


fun Router.get(path: String, rctx:RoutingContext.() -> Unit) = get(path).handler { it.rctx() }
fun Router.post(path: String, rctx:RoutingContext.() -> Unit) = post(path).handler { it.rctx() }
fun Router.put(path: String, rctx:RoutingContext.() -> Unit) = put(path).handler { it.rctx() }
fun Router.delete(path: String, rctx:RoutingContext.() -> Unit) = delete(path).handler { it.rctx() }



fun RoutingContext.param(name: String): String =
        request().getParam(name)

fun RoutingContext.bodyAs<T>(clazz: KClass<out Any>): T =
        GSON.fromJson(bodyAsString, clazz.java) as T

fun RoutingContext.send<T>(future: Future<T>) {
    future.setHandler {
        if (it.succeeded()) {
            val res = if (it.result() == null) "" else GSON.toJson(it.result())
            response().end(res)
        } else {
            response().setStatusCode(500).end(it.cause().toString())
        }
    }
}

fun HttpServer.restAPI(vertx: Vertx, body: Router.() -> Unit): HttpServer {
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())  // Required for RoutingContext.bodyAsString
    router.body()
    requestHandler { router.accept(it) }
    return this
}