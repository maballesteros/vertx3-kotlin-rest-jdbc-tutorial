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
 * Step04 - JDBC backed REST User repository
 */

object Vertx3KotlinRestJdbcTutorial {
    val gson = Gson()

    @JvmStatic fun main(args: Array<String>) {
        val port = 9000
        val vertx = Vertx.vertx()

        val client = JDBCClient.createShared(vertx, JsonObject()
                .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
                .put("driver_class", "org.hsqldb.jdbcDriver")
                .put("max_pool_size", 30));
        val userService = JdbcUserService(client)

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


class JdbcUserService(private val client: JDBCClient): UserService {

    init {
        client.execute("""
        CREATE TABLE USERS
            (ID VARCHAR(25) NOT NULL,
            FNAME VARCHAR(25) NOT NULL,
            LNAME VARCHAR(25) NOT NULL)
        """).setHandler {
            val user = User("1", "user1_fname", "user1_lname")
            addUser(user)
            println("Added user $user")
        }
    }

    override fun getUser(id: String): Future<User> =
        client.queryOne("SELECT ID, FNAME, LNAME FROM USERS WHERE ID=?", listOf(id)) {
            it.results.map { User(it.getString(0), it.getString(1), it.getString(2)) }.first()
        }


    override fun addUser(user: User): Future<Unit> =
        client.update("INSERT INTO USERS (ID, FNAME, LNAME) VALUES (?, ?, ?)",
                listOf(user.id, user.fname, user.lname))


    override fun remUser(id: String): Future<Unit> =
        client.update("DELETE FROM USERS WHERE ID = ?", listOf(id))
}