package io.javalin.openapi.plugin.redoc

import io.javalin.Javalin
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginLifecycleInit
import io.javalin.security.RouteRole

class ReDocConfiguration {
    /** Page title */
    var title = "OpenApi documentation"
    /** ReDoc route */
    var uiPath = "/redoc"
    /* Roles permitted to access ReDoc UI */
    var roles: Array<RouteRole> = emptyArray()
    /** Location of OpenApi documentation */
    var documentationPath = "/openapi"
    /** ReDoc Bundle version **/
    var version = "2.0.0-rc.70"
    /** ReDoc WebJar route */
    var webJarPath = "/webjars/redoc"
}

open class ReDocPlugin @JvmOverloads constructor(private val configuration: ReDocConfiguration = ReDocConfiguration()) : Plugin, PluginLifecycleInit {

    override fun init(app: Javalin) {}

    override fun apply(app: Javalin) {
        val reDocHandler = ReDocHandler(
            title = configuration.title,
            documentationPath = configuration.documentationPath,
            version = configuration.version,
            basePath = app.cfg.routing.contextPath
        )

        app
            .get(configuration.uiPath, reDocHandler, *configuration.roles)
            .get("${configuration.webJarPath}/*", ReDocWebJarHandler(configuration.webJarPath), *configuration.roles)
    }

}