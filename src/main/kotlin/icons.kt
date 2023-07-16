package com.github.clojure_lsp.intellij

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object Icons {
  @JvmField val CLOJURE = IconLoader.getIcon("/icons/clojure.svg", Icons::class.java)
  @JvmField val STATUS_CONNECTED = IconLoader.getIcon("/icons/clojure.svg", Icons::class.java)
  @JvmField val STATUS_DISCONNECTED = IconLoader.getIcon("/icons/clojure-mono.svg", Icons::class.java)
}
