import com.google.gson.Gson
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.util.*
import kotlin.reflect.KClass

/**
 * Step02 - In memory REST User repository
 */

object Vertx3KotlinRestJdbcTutorial {
    val gson = Gson()

    @JvmStatic fun main(args: Array<String>) {
        val port = 9000
        val vertx = Vertx.vertx()
        val server = vertx.createHttpServer()
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())  // Required for RoutingContext.bodyAsString
        val userService = MemoryUserService()

        router.get("/:userId").handler { ctx ->
            val userId = ctx.request().getParam("userId")
            userService.getUser(userId){ jsonResponse(ctx, it) }
        }

        router.post("/").handler { ctx ->
            userService.addUser(jsonRequest(ctx, User::class)) { jsonResponse(ctx, it) }
        }

        router.delete("/:userId").handler { ctx ->
            val userId = ctx.request().getParam("userId")
            userService.remUser(userId) { jsonResponse(ctx, it) }
        }

        server.requestHandler { router.accept(it) }.listen(port) {
            if (it.succeeded()) println("Server listening at $port")
            else println(it.cause())
        }
    }

    fun jsonRequest<T>(ctx: RoutingContext, clazz: KClass<out Any>): T =
        gson.fromJson(ctx.bodyAsString, clazz.java) as T


    fun jsonResponse<T>(ctx: RoutingContext, future: Future<T>) {
        if (future.succeeded()) {
            val res = if (future.result() == null) "" else gson.toJson(future.result())
            ctx.response().end(res)
        } else {
            ctx.response().setStatusCode(500).end(future.cause().toString())
        }
    }
}



//-----------------------------------------------------------------------------
// API

data class User(val id:String, val fname: String, val lname: String)

interface UserService {

    fun getUser(id: String, future: (Future<User>) -> Unit)
    fun addUser(user: User, future: (Future<Unit>) -> Unit)
    fun remUser(id: String, future: (Future<Unit>) -> Unit)
}



//-----------------------------------------------------------------------------
// IMPLEMENTATION

class MemoryUserService(): UserService {

    val _users = HashMap<String, User>()

    init {
        addUser(User("1", "user1_fname", "user1_lname"),{})
    }

    override fun getUser(id: String, future: (Future<User>) -> Unit) {
        if (_users.containsKey(id)) future(Future.succeededFuture(_users.getOrImplicitDefault(id)))
        else future(Future.failedFuture(IllegalArgumentException("Unknown user $id")))
    }

    override fun addUser(user: User, future: (Future<Unit>) -> Unit) {
        _users.put(user.id, user)
        future(Future.succeededFuture())
    }

    override fun remUser(id: String, future: (Future<Unit>) -> Unit) {
        _users.remove(id)
        future(Future.succeededFuture())
    }
}