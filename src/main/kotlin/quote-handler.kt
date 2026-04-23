package com.github.clojure_lsp.intellij.extension

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.psi.TokenType
import com.github.clojure_lsp.intellij.ClojureTokens

class QuoteHandler : SimpleTokenSetQuoteHandler(ClojureTokens.STRINGS, TokenType.BAD_CHARACTER)
