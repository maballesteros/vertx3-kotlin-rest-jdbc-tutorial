import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient

/**
 * Step05 - Promisified JDBC backed REST User repository
 */

val dbConfig = JsonObject()
        .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
        .put("driver_class", "org.hsqldb.jdbcDriver")
        .put("max_pool_size", 30)

object Vertx3KotlinRestJdbcTutorial {

    @JvmStatic fun main(args: Array<String>) {
        val vertx = promisedVertx()

        val client = JDBCClient.createShared(vertx, dbConfig);
        val userService = JdbcUserService(client)

        vertx.restApi(9000) {

            get("/:userId") { send(userService.getUser(param("userId"))) }

            post("/") { send(userService.addUser(bodyAs(User::class))) }

            delete("/:userId") { send(userService.remUser(param("userId"))) }

        }
    }
}



//-----------------------------------------------------------------------------
// API

data class User(val id:String, val fname: String, val lname: String)

interface UserService {

    fun getUser(id: String): Promise<User?>
    fun addUser(user: User): Promise<Unit>
    fun remUser(id: String): Promise<Unit>
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
        """).then {
            val user = User("1", "user1_fname", "user1_lname")
            addUser(user).then {
                println("Added user $user")
            }
        }
    }

    override fun getUser(id: String): Promise<User?> =
        client.queryOne("SELECT ID, FNAME, LNAME FROM USERS WHERE ID=?", listOf(id)) {
            User(it.getString(0), it.getString(1), it.getString(2))
        }


    override fun addUser(user: User): Promise<Unit> =
        client.update("INSERT INTO USERS (ID, FNAME, LNAME) VALUES (?, ?, ?)",
                listOf(user.id, user.fname, user.lname)).then {  }


    override fun remUser(id: String): Promise<Unit> =
        client.update("DELETE FROM USERS WHERE ID = ?", listOf(id)).then {  }
}