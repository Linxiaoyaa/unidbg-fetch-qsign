package moe.fuqiuluo.api

import CONFIG
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import moe.fuqiuluo.comm.Protocol
import moe.fuqiuluo.comm.UnidbgConfig
import project.BuildConfig

@Serializable
data class APIResult<T>(
    val code: Int,
    val msg: String = "",
    @Contextual
    val data: T? = null
)

@Serializable
data class APIInfo(
    val version: String,
    val protocol: Protocol,
    val unidbg: UnidbgConfig
)

fun Routing.index() {
    get("/") {
        call.respond(APIResult(0, "林晓雅", APIInfo(
            version = BuildConfig.version,
            protocol = CONFIG.protocol,
            unidbg = CONFIG.unidbg
        )))
    }
}
