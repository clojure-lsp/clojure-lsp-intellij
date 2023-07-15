package com.github.clojure_lsp.intellij.language.parser

import com.intellij.lang.*
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.Lexer
import com.intellij.lexer.LookAheadLexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.TokenType
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.github.clojure_lsp.intellij.ClojureTokens
import com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*

/**
 * @author gregsh
 */
class ClojureLexer(language: Language) : LookAheadLexer(FlexAdapter(_ClojureLexer(language))) {
  override fun lookAhead(baseLexer: Lexer) {
    val tokenType0 = baseLexer.tokenType
    val tokenEnd0 = baseLexer.tokenEnd
    when (tokenType0) {
      in ClojureTokens.LITERALS -> {
        baseLexer.advance()
        val tokenType = baseLexer.tokenType
        if (tokenType0 == C_NUMBER && ClojureTokens.SYM_ALIKE.contains(tokenType) ||
            tokenType0 == C_CHAR && (tokenType == C_SYM || ClojureTokens.LITERALS.contains(tokenType))) {
          advanceAs(baseLexer, TokenType.BAD_CHARACTER)
        }
        else {
          addToken(tokenEnd0, tokenType0)
        }
      }
      else -> super.lookAhead(baseLexer)
    }
  }
}

fun IElementType?.wsOrComment() = this != null && (ClojureTokens.WHITESPACES.contains(this) || ClojureTokens.COMMENTS.contains(this))

class ClojureParserUtil {
  @Suppress("UNUSED_PARAMETER")
  companion object {
    @JvmStatic
    fun adapt_builder_(root: IElementType, builder: PsiBuilder, parser: PsiParser, extendsSets: Array<TokenSet>?): PsiBuilder =
        GeneratedParserUtilBase.adapt_builder_(root, builder, parser, extendsSets).apply {
          (this as? GeneratedParserUtilBase.Builder)?.state?.braces = null
        }

    @JvmStatic
    fun parseTree(b: PsiBuilder, l: Int, p: GeneratedParserUtilBase.Parser) =
        GeneratedParserUtilBase.parseAsTree(GeneratedParserUtilBase.ErrorState.get(b), b, l,
            GeneratedParserUtilBase.DUMMY_BLOCK, false, p, GeneratedParserUtilBase.TRUE_CONDITION)

    @JvmStatic
    fun nospace(b: PsiBuilder, l: Int): Boolean {
      if (space(b, l)) {
        b.mark().apply { b.tokenType; error("no <whitespace> allowed") }
            .setCustomEdgeTokenBinders(WhitespacesBinders.GREEDY_LEFT_BINDER, WhitespacesBinders.GREEDY_RIGHT_BINDER)
      }
      return true
    }

    @JvmStatic
    fun space(b: PsiBuilder, l: Int): Boolean {
      return b.rawLookup(0).wsOrComment() || b.rawLookup(-1).wsOrComment()
    }

    private val RECOVER_SET = TokenSet.orSet(
        ClojureTokens.SHARPS, ClojureTokens.MACROS, ClojureTokens.PAREN_ALIKE, ClojureTokens.LITERALS,
        TokenSet.create(C_DOT, C_DOTDASH, C_SYM))

    @JvmStatic
    fun formRecover(b: PsiBuilder, l: Int): Boolean {
        return !RECOVER_SET.contains(b.tokenType)
    }

    @JvmStatic
    fun rootFormRecover(b: PsiBuilder, l: Int): Boolean {
      val type = b.tokenType
      return ClojureTokens.PAREN2_ALIKE.contains(type) || !RECOVER_SET.contains(type)
    }
  }
}
