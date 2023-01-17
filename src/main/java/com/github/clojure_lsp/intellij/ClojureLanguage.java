package com.github.clojure_lsp.intellij;

import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.Nullable;

public final class ClojureLanguage extends Language {
  public static final Language INSTANCE = new ClojureLanguage();

  public static final String CLOJURE_MIME_TYPE = "application/clojure";

  private ClojureLanguage() {
    super("Clojure", CLOJURE_MIME_TYPE);
  }

  @Nullable
  @Override
  public LanguageFileType getAssociatedFileType() {
      return ClojureFileType.INSTANCE;
  }
}
