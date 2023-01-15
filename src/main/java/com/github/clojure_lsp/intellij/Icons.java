package com.github.clojure_lsp.intellij;

import com.intellij.openapi.util.IconLoader;
import javax.swing.Icon;

public interface Icons {
  Icon StatusConnected = IconLoader.getIcon("/clojure-lsp/icons/status-connected.svg", Icons.class);
  Icon StatusDisconnected = IconLoader.getIcon("/clojure-lsp/icons/status-disconnected.svg", Icons.class);
}
