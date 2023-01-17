package com.github.clojure_lsp.intellij;

import javax.swing.Icon;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.NotNull;

public final class ClojureFileType extends LanguageFileType {
  public static final LanguageFileType INSTANCE = new ClojureFileType();
  public static final String DEFAULT_EXTENSION = "clj";

  private ClojureFileType() {
    super(ClojureLanguage.INSTANCE);
  }

  @NotNull
  @Override
  public String getName() {
    return "Clojure";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Clojure file";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return DEFAULT_EXTENSION;
  }

  @Override
  public Icon getIcon() {
    return Icons.ClojureFile;
  }
}
