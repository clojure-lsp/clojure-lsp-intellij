package com.github.clojure_lsp.intellij.extension

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import com.github.clojure_lsp.intellij.ClojureTokens

class QuoteHandler : SimpleTokenSetQuoteHandler(
  TokenSet.orSet(ClojureTokens.STRINGS, TokenSet.create(TokenType.BAD_CHARACTER))
)
