(ns com.github.clojure-lsp.intellij.extension.color-settings-page
  (:gen-class
   :name com.github.clojure_lsp.intellij.extension.ColorSettingsPage
   :implements [com.intellij.openapi.options.colors.RainbowColorSettingsPage])
  (:import
   [com.github.clojure_lsp.intellij ClojureLanguage]
   [com.github.clojure_lsp.intellij Icons]
   [com.github.clojure_lsp.intellij.language ClojureColors]
   [com.github.clojure_lsp.intellij.language ClojureSyntaxHighlighter]
   [com.intellij.codeHighlighting RainbowHighlighter]
   [com.intellij.openapi.editor.colors TextAttributesKey]
   [com.intellij.openapi.options.colors ColorDescriptor]
   [com.intellij.openapi.options.colors AttributesDescriptor]))

(set! *warn-on-reflection* true)

(def descriptors
  (->> {"Comments//Line comment" ClojureColors/LINE_COMMENT
        "Comments//Form comment" ClojureColors/FORM_COMMENT
        "Literals//Symbol" ClojureColors/SYMBOL
        "Literals//String" ClojureColors/STRING
        "Literals//Character" ClojureColors/CHARACTER
        "Literals//Number" ClojureColors/NUMBER
        "Literals//Boolean" ClojureColors/BOOLEAN
        "Literals//nil" ClojureColors/NIL
        "Literals//Quoted symbol" ClojureColors/QUOTED_SYM
        "Literals//Keyword" ClojureColors/KEYWORD
        "Punctuation//Comma" ClojureColors/COMMA
        "Punctuation//Dot" ClojureColors/DOT
        "Punctuation//Slash" ClojureColors/SLASH
        "Punctuation//Quote" ClojureColors/QUOTE
        "Punctuation//Syntax quote" ClojureColors/SYNTAX_QUOTE
        "Punctuation//Unquote" ClojureColors/UNQUOTE
        "Punctuation//Dereference" ClojureColors/DEREF
        "Grouping//Parens" ClojureColors/PARENS
        "Grouping//Braces" ClojureColors/BRACES
        "Grouping//Brackets" ClojureColors/BRACKETS
        "Entities//Callable (list head)" ClojureColors/CALLABLE
        "Entities//Var definition" ClojureColors/DEFINITION
        "Entities//Data reader (tag)" ClojureColors/DATA_READER
        "Entities//Function argument" ClojureColors/FN_ARGUMENT
        "Entities//Local binding" ClojureColors/LET_BINDING
        "Entities//Type field" ClojureColors/TYPE_FIELD
        "Entities//Namespace" ClojureColors/NAMESPACE
        "Entities//Alias" ClojureColors/ALIAS
        "Entities//Dynamic" ClojureColors/DYNAMIC
        "Entities//Metadata" ClojureColors/METADATA
        "Entities//Reader macro" ClojureColors/READER_MACRO
        "Java//Class" ClojureColors/JAVA_CLASS
        "Java//Static method" ClojureColors/JAVA_STATIC_METHOD
        "Java//Static field" ClojureColors/JAVA_STATIC_FIELD
        "Java//Instance method" ClojureColors/JAVA_INSTANCE_METHOD
        "Java//Instance field" ClojureColors/JAVA_INSTANCE_FIELD}
       (mapv #(AttributesDescriptor. ^String (first %) ^TextAttributesKey (second %)))
       (into-array AttributesDescriptor)))

(defn -getLanguage [_] (ClojureLanguage/INSTANCE))

(defn -isRainbowType [_ ^TextAttributesKey key]
  (or (= key ClojureColors/FN_ARGUMENT)
      (= key ClojureColors/LET_BINDING)))

(defn -getDisplayName [_] "Clojure")

(defn -getIcon [_] Icons/CLOJURE)

(defn -getAttributeDescriptors [_] descriptors)

(defn -getColorDescriptors [_] ColorDescriptor/EMPTY_ARRAY)

(defn -getHighlighter [_] (ClojureSyntaxHighlighter. (ClojureLanguage/INSTANCE)))

(defn -getAdditionalHighlightingTagToDescriptorMap [_]
  (merge {"ns" ClojureColors/NAMESPACE
          "def" ClojureColors/DEFINITION
          "alias" ClojureColors/ALIAS
          "keyword" ClojureColors/KEYWORD
          "sym" ClojureColors/QUOTED_SYM
          "dynamic" ClojureColors/DYNAMIC
          "data-reader" ClojureColors/DATA_READER
          "fn-arg" ClojureColors/FN_ARGUMENT
          "binding" ClojureColors/LET_BINDING
          "form-comment" ClojureColors/FORM_COMMENT
          "call" ClojureColors/CALLABLE
          "str" ClojureColors/STRING
          "java-class" ClojureColors/JAVA_CLASS
          "java-static-method" ClojureColors/JAVA_STATIC_METHOD
          "java-static-field" ClojureColors/JAVA_STATIC_FIELD
          "java-instance-field" ClojureColors/JAVA_INSTANCE_FIELD
          "java-instance-method" ClojureColors/JAVA_INSTANCE_METHOD}
         (RainbowHighlighter/createRainbowHLM)))

(defn -getDemoText [_]
  "
(ns <ns>my-awesome-namespace</ns>
  (<keyword>:require</keyword>
   [clojure.string <keyword>:as</keyword> string]
   [other.cool.ns <keyword>:refer</keyword> [some-function]]))

;; Literals
'symbol
<str>\"string\"</str>
\\c
123
true
nil
`quoted
:keyword

<form-comment>(<call>comment</call>
  (<call>some-function</call> 1 <keyword>:foo</keyword> <str>\"bar\"</str>))</form-comment>

(defn ^{<keyword>:deprecated</keyword> <str>\"2.0\"</str>} <def>do-something</def>
  [<fn-arg>local-a</fn-arg> <fn-arg>local-b</fn-arg>]
  (let [<binding>msg</binding> [<fn-arg>local-a</fn-arg> <fn-arg>local-b</fn-arg>]]
    (<alias>string</alias>/join <str>\"\"</str> <binding>msg</binding>)))

;; java
(<java-class>java.util.Date</java-class>.)
(<java-class>Thread</java-class>/<java-instance-method>sleep</java-instance-method> 100)
<java-class>MyClass</java-class>/<java-static-field>FIELD</java-static-field>
(<java-class>MyClass</java-class>/<java-static-method>METHOD</java-static-method>)

;; reader conditionals and dynamic resolve
#?(:clj     Double/NaN
   :cljs    <dynamic>js</dynamic>/NaN
   :default nil)
(def <def>INIT</def> <data-reader>#js</data-reader> {})
")

(defn -getPreviewEditorCustomizer [_])
(defn -customizeColorScheme [_ scheme] scheme)
(defn -getAdditionalInlineElementToDescriptorMap [_])
(defn -getAdditionalHighlightingTagToColorKeyMap [_])
