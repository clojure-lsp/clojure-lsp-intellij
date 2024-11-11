/*
 * Copyright 2016-present Greg Shrago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.github.clojure_lsp.intellij.language

import com.intellij.codeHighlighting.RainbowHighlighter
import com.intellij.lang.Language
import com.intellij.lang.LanguageUtil
import com.intellij.lexer.Lexer
import com.intellij.lexer.LookAheadLexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.RainbowColorSettingsPage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

import java.util.concurrent.ConcurrentHashMap

import com.github.clojure_lsp.intellij.ClojureTokens
import com.github.clojure_lsp.intellij.ClojureLanguage
import com.github.clojure_lsp.intellij.language.parser.ClojureLexer
import com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*

// TODO migrate to clojure extension in com.github.clojure-lsp.intellij.extension.syntax-highlighter
object ClojureColors {
  @JvmField val LINE_COMMENT = createTextAttributesKey("C_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  @JvmField val FORM_COMMENT = createTextAttributesKey("C_FORM_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
  @JvmField val STRING = createTextAttributesKey("C_STRING", DefaultLanguageHighlighterColors.STRING)
  @JvmField val CHARACTER = createTextAttributesKey("C_CHARACTER", DefaultLanguageHighlighterColors.STRING)
  @JvmField val NUMBER = createTextAttributesKey("C_NUMBER", DefaultLanguageHighlighterColors.METADATA)
  @JvmField val KEYWORD = createTextAttributesKey("C_KEYWORD", DefaultLanguageHighlighterColors.NUMBER)
  @JvmField val SYMBOL = createTextAttributesKey("C_SYMBOL", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val BOOLEAN = createTextAttributesKey("C_BOOLEAN", DefaultLanguageHighlighterColors.NUMBER)
  @JvmField val NIL = createTextAttributesKey("C_NIL", DefaultLanguageHighlighterColors.NUMBER)
  @JvmField val CALLABLE = createTextAttributesKey("C_CALLABLE", DefaultLanguageHighlighterColors.KEYWORD)

  @JvmField val COMMA = createTextAttributesKey("C_COMMA", DefaultLanguageHighlighterColors.COMMA)
  @JvmField val DOT = createTextAttributesKey("C_DOT", DefaultLanguageHighlighterColors.COMMA)
  @JvmField val SLASH = createTextAttributesKey("C_SLASH", DefaultLanguageHighlighterColors.COMMA)
  @JvmField val QUOTE = createTextAttributesKey("C_QUOTE", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val SYNTAX_QUOTE = createTextAttributesKey("C_SYNTAX_QUOTE", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val UNQUOTE = createTextAttributesKey("C_UNQUOTE", DefaultLanguageHighlighterColors.OPERATION_SIGN)
  @JvmField val DEREF = createTextAttributesKey("C_DEREF", DefaultLanguageHighlighterColors.OPERATION_SIGN)
  @JvmField val PARENS = createTextAttributesKey("C_PARENS", DefaultLanguageHighlighterColors.PARENTHESES)
  @JvmField val BRACES = createTextAttributesKey("C_BRACES", DefaultLanguageHighlighterColors.BRACES)
  @JvmField val BRACKETS = createTextAttributesKey("C_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)

  @JvmField val QUOTED_SYM = createTextAttributesKey("C_QUOTED_SYM", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val METADATA = createTextAttributesKey("C_METADATA")
  @JvmField val READER_MACRO = createTextAttributesKey("C_READER_MACRO")
  @JvmField val DATA_READER = createTextAttributesKey("C_DATA_READER", DefaultLanguageHighlighterColors.LABEL)
  @JvmField val DEFINITION = createTextAttributesKey("C_DEFINITION", DefaultLanguageHighlighterColors.FUNCTION_DECLARATION)
  @JvmField val FN_ARGUMENT = createTextAttributesKey("C_FN_ARGUMENT", DefaultLanguageHighlighterColors.PARAMETER)
  @JvmField val LET_BINDING = createTextAttributesKey("C_LET_BINDING", DefaultLanguageHighlighterColors.LOCAL_VARIABLE)
  @JvmField val TYPE_FIELD = createTextAttributesKey("C_TYPE_FIELD", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
  @JvmField val NAMESPACE = createTextAttributesKey("C_NAMESPACE", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val ALIAS = createTextAttributesKey("C_ALIAS", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
  @JvmField val DYNAMIC = createTextAttributesKey("C_DYNAMIC", DefaultLanguageHighlighterColors.PREDEFINED_SYMBOL)

  @JvmField val JAVA_CLASS = createTextAttributesKey("C_JAVA_CLASS", DefaultLanguageHighlighterColors.INSTANCE_FIELD)
  @JvmField val JAVA_STATIC_METHOD = createTextAttributesKey("C_JAVA_STATIC_METHOD", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val JAVA_STATIC_FIELD = createTextAttributesKey("C_JAVA_STATIC_FIELD", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val JAVA_INSTANCE_FIELD = createTextAttributesKey("C_JAVA_INSTANCE_FIELD", DefaultLanguageHighlighterColors.IDENTIFIER)
  @JvmField val JAVA_INSTANCE_METHOD = createTextAttributesKey("C_JAVA_INSTANCE_METHOD", DefaultLanguageHighlighterColors.IDENTIFIER)

  @JvmField val NS_COLORS: Map<String, TextAttributes> = ConcurrentHashMap()
}

class ClojureSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
  override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?) =
      ClojureSyntaxHighlighter((if (project == null) null else LanguageUtil.getLanguageForPsi(project, virtualFile)) ?: ClojureLanguage)
}

class ClojureSyntaxHighlighter(val language: Language) : SyntaxHighlighterBase() {
  override fun getHighlightingLexer() = ClojureHighlightingLexer(language)

  override fun getTokenHighlights(tokenType: IElementType?): Array<out TextAttributesKey> {
    return when (tokenType) {
      TokenType.BAD_CHARACTER -> pack(HighlighterColors.BAD_CHARACTER)
      ClojureTokens.LINE_COMMENT -> pack(ClojureColors.LINE_COMMENT)
      ClojureTokens.FORM_COMMENT -> pack(ClojureColors.FORM_COMMENT)
      C_STRING -> pack(ClojureColors.STRING)
      C_CHAR -> pack(ClojureColors.CHARACTER)
      C_NUMBER, C_HEXNUM, C_RDXNUM, C_RATIO -> pack(ClojureColors.NUMBER)
      C_BOOL -> pack(ClojureColors.BOOLEAN)
      C_NIL -> pack(ClojureColors.NIL)
      C_COLON -> pack(ClojureColors.KEYWORD)
      C_COLONCOLON -> pack(ClojureColors.KEYWORD)
      C_SYM -> pack(ClojureColors.SYMBOL)
      C_COMMA -> pack(ClojureColors.COMMA)
      C_DOT, C_DOTDASH -> pack(ClojureColors.DOT)
      C_SLASH -> pack(ClojureColors.SLASH)
      C_QUOTE -> pack(ClojureColors.QUOTE)
      C_SYNTAX_QUOTE -> pack(ClojureColors.SYNTAX_QUOTE)
      C_TILDE, C_TILDE_AT -> pack(ClojureColors.UNQUOTE)
      C_AT -> pack(ClojureColors.DEREF)
      C_HAT, C_SHARP_HAT -> pack(ClojureColors.METADATA)
      C_SHARP, C_SHARP_COMMENT, C_SHARP_EQ, C_SHARP_NS -> pack(ClojureColors.READER_MACRO)
      C_SHARP_QMARK, C_SHARP_QMARK_AT, C_SHARP_QUOTE -> pack(ClojureColors.READER_MACRO)
      C_PAREN1, C_PAREN2 -> pack(ClojureColors.PARENS)
      C_BRACE1, C_BRACE2 -> pack(ClojureColors.BRACES)
      C_BRACKET1, C_BRACKET2 -> pack(ClojureColors.BRACKETS)
      ClojureHighlightingLexer.CALLABLE -> pack(ClojureColors.CALLABLE)
      ClojureHighlightingLexer.KEYWORD -> pack(ClojureColors.KEYWORD)
      ClojureHighlightingLexer.CALLABLE_KEYWORD -> pack(ClojureColors.CALLABLE, ClojureColors.KEYWORD)
      ClojureHighlightingLexer.QUOTED_SYM -> pack(ClojureColors.QUOTED_SYM)
      ClojureHighlightingLexer.DATA_READER -> pack(ClojureColors.DATA_READER)
      ClojureHighlightingLexer.HAT_SYM -> pack(ClojureColors.METADATA)
      else -> TextAttributesKey.EMPTY_ARRAY
    }
  }
}

class ClojureHighlightingLexer(language: Language) : LookAheadLexer(ClojureLexer(language)) {
  companion object {
    val CALLABLE = IElementType("C_CALLABLE*", ClojureLanguage)
    val KEYWORD = IElementType("C_KEYWORD*", ClojureLanguage)
    val CALLABLE_KEYWORD = IElementType("C_CALLABLE_KEYWORD*", ClojureLanguage)
    val HAT_SYM = IElementType("C_HAT_SYM*", ClojureLanguage)
    val QUOTED_SYM = IElementType("C_QUOTED_SYM*", ClojureLanguage)
    val DATA_READER = IElementType("C_DATA_READER*", ClojureLanguage)
  }

  override fun lookAhead(baseLexer: Lexer) {
    fun skipWs(l: Lexer) {
      while (ClojureTokens.WHITESPACES.contains(l.tokenType)
          || ClojureTokens.COMMENTS.contains(l.tokenType)) {
        advanceLexer(l)
      }
    }

    val tokenType0 = baseLexer.tokenType

    when (tokenType0) {
      C_SHARP -> {
        baseLexer.advance()
        when (baseLexer.tokenType) {
          C_STRING, C_PAREN1, C_BRACE1 -> advanceAs(baseLexer, baseLexer.tokenType)
          C_SYM -> advanceAs(baseLexer, DATA_READER)
          else -> addToken(baseLexer.tokenStart, C_SHARP)
        }
      }
      C_QUOTE, C_SYNTAX_QUOTE -> {
        advanceAs(baseLexer, tokenType0)
        skipWs(baseLexer)
        if (baseLexer.tokenType === C_SYM) advanceSymbolAs(baseLexer, QUOTED_SYM)
        else advanceLexer(baseLexer)
      }
      C_COLON, C_COLONCOLON -> {
        advanceAs(baseLexer, tokenType0)
        if (baseLexer.tokenType === C_SYM) {
          advanceAs(baseLexer, KEYWORD)
          if (baseLexer.tokenType === C_SLASH) advanceAs(baseLexer, KEYWORD)
          if (baseLexer.tokenType === C_SYM) advanceAs(baseLexer, KEYWORD)
        }
      }
      C_PAREN1 -> {
        advanceAs(baseLexer, tokenType0)
        skipWs(baseLexer)
        val callableType = if (baseLexer.tokenType.let { it == C_COLON || it == C_COLONCOLON }) CALLABLE_KEYWORD else CALLABLE
        advanceSymbolAs(baseLexer, callableType)
      }
      C_HAT -> {
        advanceAs(baseLexer, tokenType0)
        skipWs(baseLexer)
        if (baseLexer.tokenType === C_SYM) advanceSymbolAs(baseLexer, HAT_SYM, true)
      }
      else -> super.lookAhead(baseLexer)
    }
  }

  private fun advanceSymbolAs(baseLexer: Lexer, type: IElementType, strict: Boolean = false) {
    w@ while (true) {
      val tokenType = baseLexer.tokenType
      when (tokenType) {
        C_DOT, C_DOTDASH -> if (!strict) advanceAs(baseLexer, tokenType) else break@w
        C_SLASH, C_SYM -> advanceAs(baseLexer, type)
        C_COLON, C_COLONCOLON -> if (!strict) advanceAs(baseLexer, type) else break@w
        else -> break@w
      }
    }
  }
}
