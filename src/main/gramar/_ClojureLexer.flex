package com.github.clojure_lsp.intellij.language.parser;

import com.intellij.lang.Language;
import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*;
import static com.github.clojure_lsp.intellij.ClojureTokens.LINE_COMMENT;
import static com.github.clojure_lsp.intellij.ClojureTokens.FORM_COMMENT;

%%

%{
  private Language myLanguage;

  public _ClojureLexer(Language language) {
    myLanguage = language;
  }
%}

%public
%class _ClojureLexer
%implements FlexLexer
%function advance
%type IElementType
%unicode

%state SYMBOL0, SYMBOL1, SYMBOL2, SYMBOL3
%state DISPATCH

WHITE_SPACE=\s+
LINE_COMMENT=;.*
FORM_COMMENT=#_\S*
STR_CHAR=[^\\\"]|\\.|\\\"
STRING=\" {STR_CHAR}* \"
// octal numbers: 023, 023N, but not 023M
NUMBER=[+-]? [0-9]+ (N | M | {NUMBER_EXP} M? | (\.[0-9]*) {NUMBER_EXP}? M?)? // N - BigInteger, M - BigDecimal
NUMBER_EXP=[eE][+-]?[0-9]+
HEXNUM=[+-]? "0x" [\da-fA-F]+ N?
RADIX=[+-]? [0-9]{1,2}r[\da-zA-Z]+
RATIO=[+-]? [0-9]+"/"[0-9]+
CHARACTER=\\([btrnf]|u[0-9a-fA-F]{4}|o[0-7]{3}|backspace|tab|newline|formfeed|return|space|.)
BAD_LITERAL=\" ([^\\\"]|\\.|\\\")*
  | [+-]? "0x" \w+

SYM_START=[[\w<>$%&=*+\-!?_|]--#\d] | ".."
SYM_PART=[.]? {SYM_CHAR} | ".."
SYM_CHAR=[\w<>$%&=*+\-!?_|'#]
SYM_ANY={SYM_CHAR} | [./]

SYM_TAIL={SYM_PART}+ (":" {SYM_PART}+)?

%%
<YYINITIAL> {
  {WHITE_SPACE}          { return WHITE_SPACE; }
  {LINE_COMMENT}         { return LINE_COMMENT; }
  {FORM_COMMENT}         { return FORM_COMMENT; }

  "#"                    { yybegin(DISPATCH); }

  "^"                    { return C_HAT; }
  "~@"                   { return C_TILDE_AT; }
  "~"                    { return C_TILDE; }
  "@"                    { return C_AT; }
  "("                    { return C_PAREN1; }
  ")"                    { return C_PAREN2; }
  "["                    { return C_BRACKET1; }
  "]"                    { return C_BRACKET2; }
  "{"                    { return C_BRACE1; }
  "}"                    { return C_BRACE2; }
  ","                    { return C_COMMA; }
  "'"                    { return C_QUOTE; }
  "`"                    { return C_SYNTAX_QUOTE; }

  "nil"                  { return C_NIL; }
  true|false             { return C_BOOL; }

  {STRING}               { return C_STRING; }
  {NUMBER}               { return C_NUMBER; }
  {HEXNUM}               { return C_HEXNUM; }
  {RADIX}                { return C_RDXNUM; }
  {RATIO}                { return C_RATIO; }
  {CHARACTER}            { return C_CHAR; }
  {BAD_LITERAL}          { return BAD_CHARACTER; }

  "::"                   { yybegin(SYMBOL0); return C_COLONCOLON; }
  ":"                    { yybegin(SYMBOL0); return C_COLON; }
  ".-"  /  {SYM_CHAR}    { yybegin(SYMBOL0); return C_DOTDASH; }
  ".-"                   { return C_SYM; }
  "."   /  {SYM_CHAR}    { yybegin(SYMBOL0); return C_DOT; }
  "."                    { return C_SYM; }
  "/" {SYM_ANY}+         { yybegin(YYINITIAL); return BAD_CHARACTER; }
  "/"                    { return C_SYM; }

  {SYM_START}{SYM_TAIL}? { yybegin(SYMBOL1); return C_SYM; }
}

<SYMBOL0> {
  {SYM_TAIL}             { yybegin(SYMBOL1); return C_SYM; }
  [^]                    { yybegin(YYINITIAL); yypushback(yylength()); }
}

<SYMBOL1> {
  ":"                    { yybegin(YYINITIAL); return BAD_CHARACTER; }
  "/"                    { yybegin(SYMBOL2); return C_SLASH; }
  "."                    { yybegin(YYINITIAL); return C_DOT; }
  [^]                    { yybegin(YYINITIAL); yypushback(yylength()); }
}

<SYMBOL2> {
  {SYM_TAIL}             { yybegin(SYMBOL3); return C_SYM; }
}

<SYMBOL2, SYMBOL3> {
  ":"                    { yybegin(YYINITIAL); return BAD_CHARACTER; }
  "."                    { yybegin(YYINITIAL); return C_DOT; }
  [^]                    { yybegin(YYINITIAL); yypushback(yylength()); }
}

<YYINITIAL, SYMBOL2, SYMBOL3> {
  "/" {SYM_ANY}+         { yybegin(YYINITIAL); return BAD_CHARACTER; }
}

<DISPATCH> {
  "^"                    { yybegin(YYINITIAL); return C_SHARP_HAT; }  // Meta
  "'"                    { yybegin(YYINITIAL); return C_SHARP_QUOTE; }  // Var
  "\""                   { yybegin(YYINITIAL); yypushback(1); return C_SHARP; }  // Regex
  "("                    { yybegin(YYINITIAL); yypushback(1); return C_SHARP; }  // Fn
  "{"                    { yybegin(YYINITIAL); yypushback(1); return C_SHARP; }  // Set
  "="                    { yybegin(YYINITIAL); return C_SHARP_EQ; }  // Eval
  "!"                    { yybegin(YYINITIAL); return C_SHARP_COMMENT; }  // Comment
  "<"                    { yybegin(YYINITIAL); return BAD_CHARACTER; }  // Unreadable
  "_"                    { yybegin(YYINITIAL); return C_SHARP_COMMENT; }  // Discard
  "?@"                   { yybegin(YYINITIAL); return C_SHARP_QMARK_AT; }  // Conditional w/ Splicing
  "?"                    { yybegin(YYINITIAL); return C_SHARP_QMARK; }  // Conditional
  "#"                    { yybegin(YYINITIAL); return C_SHARP_SYM; }  // Conditional
  ":"                    { yybegin(YYINITIAL); yypushback(yylength()); return C_SHARP_NS; }  // Map ns prefix
  [\s\w]                 { yybegin(YYINITIAL); yypushback(yylength()); return C_SHARP; }
  [^]                    { yybegin(YYINITIAL); yypushback(yylength()); return BAD_CHARACTER; }

  <<EOF>>                { yybegin(YYINITIAL); return BAD_CHARACTER; }
}

[^] { return BAD_CHARACTER; }
