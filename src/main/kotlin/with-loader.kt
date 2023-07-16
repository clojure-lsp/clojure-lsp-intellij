package com.github.clojure_lsp.intellij

class WithLoader {
    companion object {
        @JvmStatic
        fun bind() {
            Thread.currentThread().setContextClassLoader(WithLoader::class.java.getClassLoader())
        }
    }
}
