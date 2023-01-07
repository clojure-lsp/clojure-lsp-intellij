package com.github.clojure_lsp.intellij;

public class WithLoader {
    static {
        bind();
    }
    public static void bind() {
        Thread.currentThread().setContextClassLoader(WithLoader.class.getClassLoader());
    }
}
