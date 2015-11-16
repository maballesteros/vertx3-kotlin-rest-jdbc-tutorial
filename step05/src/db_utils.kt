import io.vertx.core.AsyncResult
import io.vertx.core.json.JsonArray
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLConnection
import io.vertx.ext.sql.UpdateResult

/**
 * Created by mike on 15/11/15.
 */

fun <T : Any> AsyncResult<T>.handle(deferred: Promise.Deferred<T>) =
        if (succeeded()) {
            deferred.resolve(result())
        }
        else {
            cause().printStackTrace()
            deferred.reject(cause())
        }

// ---------------------------------------------------------------------------
// Promisified Vertx SQL API

fun JDBCClient.getConnection(): Promise<SQLConnection> {
    val deferred = Promise.Deferred<SQLConnection>()
    getConnection { it.handle(deferred) }
    return deferred.promise
}

fun SQLConnection.queryWithParams(query: String, params: JsonArray): Promise<ResultSet> {
    val deferred = Promise.Deferred<ResultSet>()
    queryWithParams(query, params) {
        it.handle(deferred)
    }
    return deferred.promise
}

fun SQLConnection.updateWithParams(query: String, params: JsonArray): Promise<UpdateResult> {
    val deferred = Promise.Deferred<UpdateResult>()
    updateWithParams(query, params) { it.handle(deferred) }
    return deferred.promise
}

fun SQLConnection.execute(query: String): Promise<Boolean> {
    val deferred = Promise.Deferred<Boolean>()
    execute(query) { if (it.succeeded()) deferred.resolve(true) else deferred.reject(it.cause()) }
    return deferred.promise
}

// ---------------------------------------------------------------------------
// Handy SQL API

fun JDBCClient.withConnection<T : Any?>(res: (SQLConnection) -> Promise<T>): Promise<T> =
        getConnection().pipe { res(it).always { it.close() } }

fun JDBCClient.query<T>(query: String, params: List<Any>, rsHandler: (ResultSet) -> List<T>): Promise<List<T>> =
        withConnection { it.queryWithParams(query, JsonArray(params)).then { rsHandler(it) } }

fun JDBCClient.queryOne<T : Any>(query: String, params: List<Any>, rsHandler: (JsonArray) -> T?): Promise<T?> =
        withConnection { it.queryWithParams(query, JsonArray(params))
                .then { rsHandler(it.results.first()) }
        }

fun JDBCClient.update(query: String, params: List<Any>): Promise<Int> =
        withConnection { it.updateWithParams(query, JsonArray(params)).then { it.updated } }


fun JDBCClient.execute(query: String): Promise<Boolean> =
        withConnection { it.execute(query).then { true } }
