// This is a generated file. Not intended for manual editing.
package com.github.clojure_lsp.intellij.language.parser;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.github.clojure_lsp.intellij.language.psi.ClojureTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;
import static com.github.clojure_lsp.intellij.language.parser.ClojureParserUtil.adapt_builder_;
import static com.github.clojure_lsp.intellij.language.parser.ClojureParserUtil.*;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class ClojureParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType root_, PsiBuilder builder_) {
    parseLight(root_, builder_);
    return builder_.getTreeBuilt();
  }

  public void parseLight(IElementType root_, PsiBuilder builder_) {
    boolean result_;
    builder_ = adapt_builder_(root_, builder_, this, EXTENDS_SETS_);
    Marker marker_ = enter_section_(builder_, 0, _COLLAPSE_, null);
    result_ = parse_root_(root_, builder_);
    exit_section_(builder_, 0, marker_, root_, result_, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType root_, PsiBuilder builder_) {
    return parse_root_(root_, builder_, 0);
  }

  static boolean parse_root_(IElementType root_, PsiBuilder builder_, int level_) {
    return root(builder_, level_ + 1);
  }

  public static final TokenSet[] EXTENDS_SETS_ = new TokenSet[] {
    create_token_set_(C_ACCESS, C_CONSTRUCTOR, C_FORM, C_FUN,
      C_KEYWORD, C_LIST, C_LITERAL, C_MAP,
      C_REGEXP, C_SET, C_SYMBOL, C_VEC),
  };

  /* ********************************************************** */
  // ('.' | '.-') symbol
  public static boolean access(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access")) return false;
    if (!nextTokenIs(builder_, "<access>", C_DOT, C_DOTDASH)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, C_ACCESS, "<access>");
    result_ = access_0(builder_, level_ + 1);
    result_ = result_ && symbol(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // '.' | '.-'
  private static boolean access_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, C_DOT);
    if (!result_) result_ = consumeToken(builder_, C_DOTDASH);
    return result_;
  }

  /* ********************************************************** */
  // !<<space>> '.'
  public static boolean access_left(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access_left")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, C_ACCESS, "<access left>");
    result_ = access_left_0(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, C_DOT);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // !<<space>>
  private static boolean access_left_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "access_left_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !space(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // &('::' sym)
  static boolean alias_condition(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "alias_condition")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = alias_condition_0(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // '::' sym
  private static boolean alias_condition_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "alias_condition_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeTokens(builder_, 0, C_COLONCOLON, C_SYM);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // "#_" form
  public static boolean commented(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "commented")) return false;
    if (!nextTokenIsFast(builder_, C_SHARP_COMMENT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_COMMENTED, null);
    result_ = consumeTokenFast(builder_, C_SHARP_COMMENT);
    pinned_ = result_; // pin = 1
    result_ = result_ && form(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // '#' skip symbol skip form
  public static boolean constructor(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "constructor")) return false;
    if (!nextTokenIs(builder_, "<form>", C_SHARP)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_CONSTRUCTOR, "<form>");
    result_ = consumeToken(builder_, C_SHARP);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, skip(builder_, level_ + 1));
    result_ = pinned_ && report_error_(builder_, symbol(builder_, level_ + 1)) && result_;
    result_ = pinned_ && report_error_(builder_, skip(builder_, level_ + 1)) && result_;
    result_ = pinned_ && form(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // form_prefix form_prefix * form_upper | form_inner
  public static boolean form(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, C_FORM, "<form>");
    result_ = form_0(builder_, level_ + 1);
    if (!result_) result_ = form_inner(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // form_prefix form_prefix * form_upper
  private static boolean form_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form_0")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = form_prefix(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, form_0_1(builder_, level_ + 1));
    result_ = pinned_ && form_upper(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // form_prefix *
  private static boolean form_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form_0_1")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!form_prefix(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "form_0_1", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // p_forms | s_forms | constructor
  static boolean form_inner(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form_inner")) return false;
    boolean result_;
    result_ = p_forms(builder_, level_ + 1);
    if (!result_) result_ = s_forms(builder_, level_ + 1);
    if (!result_) result_ = constructor(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // metadata | reader_macro | commented
  static boolean form_prefix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form_prefix")) return false;
    boolean result_;
    result_ = metadata(builder_, level_ + 1);
    if (!result_) result_ = reader_macro(builder_, level_ + 1);
    if (!result_) result_ = commented(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // form_inner
  public static boolean form_upper(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "form_upper")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_ | _UPPER_, C_FORM, "<form>");
    result_ = form_inner(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '#' <<nospace>> '(' list_body ')'
  public static boolean fun(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "fun")) return false;
    if (!nextTokenIs(builder_, C_SHARP)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_FUN, null);
    result_ = consumeToken(builder_, C_SHARP);
    result_ = result_ && nospace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, C_PAREN1);
    pinned_ = result_; // pin = '[\(\[\{]'
    result_ = result_ && report_error_(builder_, list_body(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, C_PAREN2) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // <<items_entry <<recover>> <<param>>>> *
  static boolean items(PsiBuilder builder_, int level_, Parser recover, Parser param) {
    if (!recursion_guard_(builder_, level_, "items")) return false;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    while (true) {
      int pos_ = current_position_(builder_);
      if (!items_entry(builder_, level_ + 1, recover, param)) break;
      if (!empty_element_parsed_guard_(builder_, "items", pos_)) break;
    }
    exit_section_(builder_, level_, marker_, true, false, recover);
    return true;
  }

  /* ********************************************************** */
  // (not_eof <<recover>>) (commented | <<param>>)
  static boolean items_entry(PsiBuilder builder_, int level_, Parser recover, Parser param) {
    if (!recursion_guard_(builder_, level_, "items_entry")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = items_entry_0(builder_, level_ + 1, recover);
    pinned_ = result_; // pin = 1
    result_ = result_ && items_entry_1(builder_, level_ + 1, param);
    exit_section_(builder_, level_, marker_, result_, pinned_, form_recover_parser_);
    return result_ || pinned_;
  }

  // not_eof <<recover>>
  private static boolean items_entry_0(PsiBuilder builder_, int level_, Parser recover) {
    if (!recursion_guard_(builder_, level_, "items_entry_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = not_eof(builder_, level_ + 1);
    result_ = result_ && recover.parse(builder_, level_);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // commented | <<param>>
  private static boolean items_entry_1(PsiBuilder builder_, int level_, Parser param) {
    if (!recursion_guard_(builder_, level_, "items_entry_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = commented(builder_, level_ + 1);
    if (!result_) result_ = param.parse(builder_, level_);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // (':' | '::') <<nospace>> symbol_qualified
  public static boolean keyword(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "keyword")) return false;
    if (!nextTokenIs(builder_, "<keyword>", C_COLON, C_COLONCOLON)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_KEYWORD, "<keyword>");
    result_ = keyword_0(builder_, level_ + 1);
    result_ = result_ && nospace(builder_, level_ + 1);
    result_ = result_ && symbol_qualified(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // ':' | '::'
  private static boolean keyword_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "keyword_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, C_COLON);
    if (!result_) result_ = consumeToken(builder_, C_COLONCOLON);
    return result_;
  }

  /* ********************************************************** */
  // '(' list_body ')'
  public static boolean list(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "list")) return false;
    if (!nextTokenIs(builder_, C_PAREN1)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_LIST, null);
    result_ = consumeToken(builder_, C_PAREN1);
    pinned_ = result_; // pin = '[\(\[\{]'
    result_ = result_ && report_error_(builder_, list_body(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, C_PAREN2) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // <<items !')' form>>
  static boolean list_body(PsiBuilder builder_, int level_) {
    return items(builder_, level_ + 1, ClojureParser::list_body_0_0, ClojureParser::form);
  }

  // !')'
  private static boolean list_body_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "list_body_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, C_PAREN2);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // number | hexnum | rdxnum | ratio | bool | nil | string | char
  public static boolean literal(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "literal")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_LITERAL, "<literal>");
    result_ = consumeToken(builder_, C_NUMBER);
    if (!result_) result_ = consumeToken(builder_, C_HEXNUM);
    if (!result_) result_ = consumeToken(builder_, C_RDXNUM);
    if (!result_) result_ = consumeToken(builder_, C_RATIO);
    if (!result_) result_ = consumeToken(builder_, C_BOOL);
    if (!result_) result_ = consumeToken(builder_, C_NIL);
    if (!result_) result_ = consumeToken(builder_, C_STRING);
    if (!result_) result_ = consumeToken(builder_, C_CHAR);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '{' map_body '}'
  public static boolean map(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map")) return false;
    if (!nextTokenIs(builder_, C_BRACE1)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_MAP, null);
    result_ = consumeToken(builder_, C_BRACE1);
    pinned_ = result_; // pin = '[\(\[\{]'
    result_ = result_ && report_error_(builder_, map_body(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, C_BRACE2) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // <<items !'}' map_entry>>
  static boolean map_body(PsiBuilder builder_, int level_) {
    return items(builder_, level_ + 1, ClojureParser::map_body_0_0, ClojureParser::map_entry);
  }

  // !'}'
  private static boolean map_body_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_body_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, C_BRACE2);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // form skip form
  static boolean map_entry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_entry")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = form(builder_, level_ + 1);
    result_ = result_ && skip(builder_, level_ + 1);
    pinned_ = result_; // pin = 2
    result_ = result_ && form(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // "#:" (':' <<nospace>> symbol_plain
  //   | alias_condition '::' <<nospace>> symbol_plain
  //   | '::' ) &'{'
  static boolean map_ns_prefix(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_ns_prefix")) return false;
    if (!nextTokenIs(builder_, C_SHARP_NS)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeToken(builder_, C_SHARP_NS);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, map_ns_prefix_1(builder_, level_ + 1));
    result_ = pinned_ && map_ns_prefix_2(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // ':' <<nospace>> symbol_plain
  //   | alias_condition '::' <<nospace>> symbol_plain
  //   | '::'
  private static boolean map_ns_prefix_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_ns_prefix_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = map_ns_prefix_1_0(builder_, level_ + 1);
    if (!result_) result_ = map_ns_prefix_1_1(builder_, level_ + 1);
    if (!result_) result_ = consumeToken(builder_, C_COLONCOLON);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // ':' <<nospace>> symbol_plain
  private static boolean map_ns_prefix_1_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_ns_prefix_1_0")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeToken(builder_, C_COLON);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, nospace(builder_, level_ + 1));
    result_ = pinned_ && symbol_plain(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // alias_condition '::' <<nospace>> symbol_plain
  private static boolean map_ns_prefix_1_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_ns_prefix_1_1")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = alias_condition(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && report_error_(builder_, consumeToken(builder_, C_COLONCOLON));
    result_ = pinned_ && report_error_(builder_, nospace(builder_, level_ + 1)) && result_;
    result_ = pinned_ && symbol_plain(builder_, level_ + 1) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // &'{'
  private static boolean map_ns_prefix_2(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "map_ns_prefix_2")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, C_BRACE1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // ("^" | "#^") (string | symbol | keyword | map)
  public static boolean metadata(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metadata")) return false;
    if (!nextTokenIs(builder_, "<form>", C_HAT, C_SHARP_HAT)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_METADATA, "<form>");
    result_ = metadata_0(builder_, level_ + 1);
    result_ = result_ && metadata_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  // "^" | "#^"
  private static boolean metadata_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metadata_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, C_HAT);
    if (!result_) result_ = consumeToken(builder_, C_SHARP_HAT);
    return result_;
  }

  // string | symbol | keyword | map
  private static boolean metadata_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "metadata_1")) return false;
    boolean result_;
    result_ = consumeToken(builder_, C_STRING);
    if (!result_) result_ = symbol(builder_, level_ + 1);
    if (!result_) result_ = keyword(builder_, level_ + 1);
    if (!result_) result_ = map(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // !<<eof>>
  static boolean not_eof(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "not_eof")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !eof(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // list | set | vec | map | fun
  static boolean p_forms(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "p_forms")) return false;
    boolean result_;
    result_ = list(builder_, level_ + 1);
    if (!result_) result_ = set(builder_, level_ + 1);
    if (!result_) result_ = vec(builder_, level_ + 1);
    if (!result_) result_ = map(builder_, level_ + 1);
    if (!result_) result_ = fun(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // ('#?' | '#?@') &'('
  static boolean reader_cond(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reader_cond")) return false;
    if (!nextTokenIs(builder_, "<form>", C_SHARP_QMARK, C_SHARP_QMARK_AT)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, null, "<form>");
    result_ = reader_cond_0(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && reader_cond_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // '#?' | '#?@'
  private static boolean reader_cond_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reader_cond_0")) return false;
    boolean result_;
    result_ = consumeToken(builder_, C_SHARP_QMARK);
    if (!result_) result_ = consumeToken(builder_, C_SHARP_QMARK_AT);
    return result_;
  }

  // &'('
  private static boolean reader_cond_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reader_cond_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, C_PAREN1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // "'" | "~" | "~@" | "@" | "`" | "#'" | "#=" | symbolic_value | reader_cond | map_ns_prefix
  public static boolean reader_macro(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "reader_macro")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_READER_MACRO, "<form>");
    result_ = consumeToken(builder_, C_QUOTE);
    if (!result_) result_ = consumeToken(builder_, C_TILDE);
    if (!result_) result_ = consumeToken(builder_, C_TILDE_AT);
    if (!result_) result_ = consumeToken(builder_, C_AT);
    if (!result_) result_ = consumeToken(builder_, C_SYNTAX_QUOTE);
    if (!result_) result_ = consumeToken(builder_, C_SHARP_QUOTE);
    if (!result_) result_ = consumeToken(builder_, C_SHARP_EQ);
    if (!result_) result_ = symbolic_value(builder_, level_ + 1);
    if (!result_) result_ = reader_cond(builder_, level_ + 1);
    if (!result_) result_ = map_ns_prefix(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '#' <<nospace>> string
  public static boolean regexp(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "regexp")) return false;
    if (!nextTokenIs(builder_, C_SHARP)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, C_SHARP);
    result_ = result_ && nospace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, C_STRING);
    exit_section_(builder_, marker_, C_REGEXP, result_);
    return result_;
  }

  /* ********************************************************** */
  // <<parseTree (root_entry)>>
  static boolean root(PsiBuilder builder_, int level_) {
    return parseTree(builder_, level_ + 1, ClojureParser::root_0_0);
  }

  // (root_entry)
  private static boolean root_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "root_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = root_entry(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  /* ********************************************************** */
  // not_eof (commented | form)
  static boolean root_entry(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "root_entry")) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = not_eof(builder_, level_ + 1);
    pinned_ = result_; // pin = 1
    result_ = result_ && root_entry_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, root_entry_recover_parser_);
    return result_ || pinned_;
  }

  // commented | form
  private static boolean root_entry_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "root_entry_1")) return false;
    boolean result_;
    result_ = commented(builder_, level_ + 1);
    if (!result_) result_ = form(builder_, level_ + 1);
    return result_;
  }

  /* ********************************************************** */
  // symbol access_left? | keyword | literal | regexp | access
  static boolean s_forms(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "s_forms")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = s_forms_0(builder_, level_ + 1);
    if (!result_) result_ = keyword(builder_, level_ + 1);
    if (!result_) result_ = literal(builder_, level_ + 1);
    if (!result_) result_ = regexp(builder_, level_ + 1);
    if (!result_) result_ = access(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // symbol access_left?
  private static boolean s_forms_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "s_forms_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = symbol(builder_, level_ + 1);
    result_ = result_ && s_forms_0_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // access_left?
  private static boolean s_forms_0_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "s_forms_0_1")) return false;
    access_left(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '#' <<nospace>> '{' set_body '}'
  public static boolean set(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "set")) return false;
    if (!nextTokenIs(builder_, C_SHARP)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_SET, null);
    result_ = consumeToken(builder_, C_SHARP);
    result_ = result_ && nospace(builder_, level_ + 1);
    result_ = result_ && consumeToken(builder_, C_BRACE1);
    pinned_ = result_; // pin = '[\(\[\{]'
    result_ = result_ && report_error_(builder_, set_body(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, C_BRACE2) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // <<items !'}' form>>
  static boolean set_body(PsiBuilder builder_, int level_) {
    return items(builder_, level_ + 1, ClojureParser::set_body_0_0, ClojureParser::form);
  }

  // !'}'
  private static boolean set_body_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "set_body_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, C_BRACE2);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // commented *
  static boolean skip(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "skip")) return false;
    while (true) {
      int pos_ = current_position_(builder_);
      if (!commented(builder_, level_ + 1)) break;
      if (!empty_element_parsed_guard_(builder_, "skip", pos_)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // symbol_qualified
  public static boolean symbol(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbol")) return false;
    if (!nextTokenIs(builder_, C_SYM)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _COLLAPSE_, C_SYMBOL, null);
    result_ = symbol_qualified(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '/' sym
  public static boolean symbol_nsq(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbol_nsq")) return false;
    if (!nextTokenIsFast(builder_, C_SLASH)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _LEFT_, C_SYMBOL, null);
    result_ = consumeTokens(builder_, 0, C_SLASH, C_SYM);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // sym
  public static boolean symbol_plain(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbol_plain")) return false;
    if (!nextTokenIs(builder_, C_SYM)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = consumeToken(builder_, C_SYM);
    exit_section_(builder_, marker_, C_SYMBOL, result_);
    return result_;
  }

  /* ********************************************************** */
  // symbol_plain symbol_nsq?
  static boolean symbol_qualified(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbol_qualified")) return false;
    if (!nextTokenIs(builder_, C_SYM)) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_);
    result_ = symbol_plain(builder_, level_ + 1);
    result_ = result_ && symbol_qualified_1(builder_, level_ + 1);
    exit_section_(builder_, marker_, null, result_);
    return result_;
  }

  // symbol_nsq?
  private static boolean symbol_qualified_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbol_qualified_1")) return false;
    symbol_nsq(builder_, level_ + 1);
    return true;
  }

  /* ********************************************************** */
  // '##' &sym
  static boolean symbolic_value(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbolic_value")) return false;
    if (!nextTokenIs(builder_, C_SHARP_SYM)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_);
    result_ = consumeToken(builder_, C_SHARP_SYM);
    pinned_ = result_; // pin = 1
    result_ = result_ && symbolic_value_1(builder_, level_ + 1);
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  // &sym
  private static boolean symbolic_value_1(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "symbolic_value_1")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _AND_);
    result_ = consumeToken(builder_, C_SYM);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  /* ********************************************************** */
  // '[' vec_body ']'
  public static boolean vec(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "vec")) return false;
    if (!nextTokenIs(builder_, C_BRACKET1)) return false;
    boolean result_, pinned_;
    Marker marker_ = enter_section_(builder_, level_, _NONE_, C_VEC, null);
    result_ = consumeToken(builder_, C_BRACKET1);
    pinned_ = result_; // pin = '[\(\[\{]'
    result_ = result_ && report_error_(builder_, vec_body(builder_, level_ + 1));
    result_ = pinned_ && consumeToken(builder_, C_BRACKET2) && result_;
    exit_section_(builder_, level_, marker_, result_, pinned_, null);
    return result_ || pinned_;
  }

  /* ********************************************************** */
  // <<items !']' form>>
  static boolean vec_body(PsiBuilder builder_, int level_) {
    return items(builder_, level_ + 1, ClojureParser::vec_body_0_0, ClojureParser::form);
  }

  // !']'
  private static boolean vec_body_0_0(PsiBuilder builder_, int level_) {
    if (!recursion_guard_(builder_, level_, "vec_body_0_0")) return false;
    boolean result_;
    Marker marker_ = enter_section_(builder_, level_, _NOT_);
    result_ = !consumeToken(builder_, C_BRACKET2);
    exit_section_(builder_, level_, marker_, result_, false, null);
    return result_;
  }

  static final Parser form_recover_parser_ = (builder_, level_) -> formRecover(builder_, level_ + 1);
  static final Parser root_entry_recover_parser_ = (builder_, level_) -> rootFormRecover(builder_, level_ + 1);
}
