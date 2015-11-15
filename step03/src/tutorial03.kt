import com.google.gson.Gson
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.util.*
import kotlin.reflect.KClass

/**
 * Step03 - In memory REST User repository (with simplified REST definitions)
 */

object Vertx3KotlinRestJdbcTutorial {
    val gson = Gson()

    @JvmStatic fun main(args: Array<String>) {
        val port = 9000
        val vertx = Vertx.vertx()
        val userService = MemoryUserService()

        vertx.createHttpServer().restAPI(vertx) {

            get("/:userId") { send(userService.getUser(param("userId"))) }

            post("/") { send(userService.addUser(bodyAs(User::class))) }

            delete("/:userId") { send(userService.remUser(param("userId"))) }

        }.listen(port) {
            if (it.succeeded()) println("Server listening at $port")
            else println(it.cause())
        }
    }
}


//-----------------------------------------------------------------------------
// API

data class User(val id:String, val fname: String, val lname: String)

interface UserService {

    fun getUser(id: String): Future<User>
    fun addUser(user: User): Future<Unit>
    fun remUser(id: String): Future<Unit>
}



//-----------------------------------------------------------------------------
// IMPLEMENTATION


class MemoryUserService(): UserService {

    val _users = HashMap<String, User>()

    init {
        addUser(User("1", "user1_fname", "user1_lname"))
    }

    override fun getUser(id: String): Future<User> {
        return if (_users.containsKey(id)) Future.succeededFuture(_users.getOrImplicitDefault(id))
        else Future.failedFuture(IllegalArgumentException("Unknown user $id"))
    }

    override fun addUser(user: User): Future<Unit> {
        _users.put(user.id, user)
        return Future.succeededFuture()
    }

    override fun remUser(id: String): Future<Unit> {
        _users.remove(id)
        return Future.succeededFuture()
    }
}