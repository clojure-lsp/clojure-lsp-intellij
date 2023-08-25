package com.github.clojure_lsp.intellij

open class ClojureClassLoader {
    companion object {
        init {
            bind()
        }

        @JvmStatic
        fun bind() {
            Thread.currentThread().setContextClassLoader(ClojureClassLoader::class.java.getClassLoader())
        }
    }
}
