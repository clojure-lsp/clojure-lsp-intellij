package com.github.clojure_lsp.intellij

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest
import org.eclipse.lsp4j.services.LanguageServer
import java.util.concurrent.CompletableFuture

interface ClojureLanguageServer : LanguageServer {

    @JsonRequest("clojure/dependencyContents")
    fun dependencyContents(params: Any): CompletableFuture<String>

    @JsonRequest("clojure/serverInfo/raw")
    fun serverInfo(): CompletableFuture<Any>
}
