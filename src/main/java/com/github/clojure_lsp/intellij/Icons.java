package com.github.clojure_lsp.intellij;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public interface Icons {
  Icon ClojureFile = IconLoader.getIcon("/clojure-lsp/icons/clojure.svg", Icons.class);
  Icon StatusConnected = IconLoader.getIcon("/clojure-lsp/icons/clojure.svg", Icons.class);
  Icon StatusDisconnected = IconLoader.getIcon("/clojure-lsp/icons/clojure-mono.svg", Icons.class);
}
