<img src="images/logo-dark.svg" width="180" align="right">

# clojure-lsp-intellij

<!-- Plugin description -->

Free OpenSource Intellij plugin with support for Clojure & ClojureScript development via the built-in Language Server (LSP) [clojure-lsp](https://clojure-lsp.io/) providing features via static analysis

<!-- Plugin description end -->

![Clojure LSP Intellij](images/clojure-lsp-intellij-1.png)

---

## Rationale

Intellij is the only mainstream editor with no good, free and dedicated support for LSP, there are already excelent plugins for Clojure like [Cursive](https://cursive-ide.com/) which provides lots of features with REPL support or [ClojureKit](https://github.com/gregsh/Clojure-Kit) which adds basic Clojure support for the language, but none uses clojure-lsp or follows the LSP standard which some users may prefer as some features are only available in clojure-lsp.

Keep in mind that this plugin provides only LSP features which relies on clojure-lsp (and clj-kondo under the hood) static analysis, so no runtime features exists, like REPL integration or support, for that a separated plugin is needed as it's not possible to use only the REPL part of other plugins like Cursive together with this plugin.

Also, this plugin does not use IntelliJ's LSP support for 2 reasons:
  - The LSP feature is only available for Ultimate Edition (paid edition), making this plugin only available for those editions.
  - The LSP feature is pretty alpha, missing some features yet, some that were already implemented in this plugin.

---

## LSP implemented capabilities

Below are all the currently supported LSP capabilities and their implementation status:

| capability                             | done | notes                               |
|----------------------------------------|------|-------------------------------------|
| initialize                             | √    |                                     |
| initialized                            | √    |                                     |
| shutdown                               | √    |                                     |
| exit                                   |      |                                     |
| $/cancelRequest                        |      |                                     |
| $/progress                             | √    |                                     |
| window/showDocument                    |      |                                     |
| window/showMessage                     |      |                                     |
| window/showMessageRequest              |      |                                     |
| window/logMessage                      |      |                                     |
| window/workDoneProgress/create         |      |                                     |
| window/workDoneProgress/cancel         |      |                                     |
| telemetry/event                        |      |                                     |
| client/registerCapability              |      |                                     |
| client/unregisterCapability            |      |                                     |
| workspace/workspaceFolders             |      |                                     |
| workspace/didChangeWorkspaceFolders    |      |                                     |
| workspace/didChangeConfiguration       |      |                                     |
| workspace/configuration                |      |                                     |
| workspace/didChangeWatchedFiles        |      |                                     |
| workspace/symbol                       |      |                                     |
| workspace/executeCommand               |      |                                     |
| workspace/applyEdit                    |      |                                     |
| workspace/willRenameFiles              |      |                                     |
| workspace/didRenameFiles               |      |                                     |
| workspace/willCreateFiles              |      |                                     |
| workspace/didCreateFiles               |      |                                     |
| workspace/willDeleteFiles              |      |                                     |
| workspace/didDeleteFiles               |      |                                     |
| textDocument/didOpen                   | √    |                                     |
| textDocument/didChange                 | √    |                                     |
| textDocument/willSave                  |      |                                     |
| textDocument/willSaveWaitUntil         |      |                                     |
| textDocument/didSave                   |      |                                     |
| textDocument/didClose                  | √    |                                     |
| textDocument/publishDiagnostics        | √    |                                     |
| textDocument/completion                |      |                                     |
| completionItem/resolve                 |      |                                     |
| textDocument/hover                     | √    |                                     |
| textDocument/signatureHelp             |      |                                     |
| textDocument/declaration               |      |                                     |
| textDocument/definition                | √    | Custom action: Via Alt + Shift + F6 |
| textDocument/typeDefinition            |      |                                     |
| textDocument/implementation            |      |                                     |
| textDocument/references                | √    | Custom action: Via Alt + Shift + F7 |
| textDocument/documentHighlight         |      |                                     |
| textDocument/documentSymbol            |      |                                     |
| textDocument/codeAction                |      |                                     |
| codeAction/resolve                     |      |                                     |
| textDocument/codeLens                  | √    |                                     |
| codeLens/resolve                       | √    |                                     |
| textDocument/documentLink              |      |                                     |
| documentLink/resolve                   |      |                                     |
| textDocument/documentColor             |      |                                     |
| textDocument/colorPresentation         |      |                                     |
| textDocument/formatting                | √    |                                     |
| textDocument/rangeFormatting           |      |                                     |
| textDocument/onTypeFormatting          |      |                                     |
| textDocument/rename                    |      |                                     |
| textDocument/prepareRename             |      |                                     |
| textDocument/foldingRange              |      |                                     |
| textDocument/selectionRange            |      |                                     |
| textDocument/semanticTokens/full       |      |                                     |
| textDocument/semanticTokens/full/delta |      |                                     |
| textDocument/semanticTokens/range      |      |                                     |
| workspace/semanticTokens/refresh       |      |                                     |
| workspace/codeLens/refresh             |      |                                     |
| textDocument/linkedEditingRange        |      |                                     |
| textDocument/prepareCallHierarchy      |      |                                     |
| callHierarchy/incomingCalls            |      |                                     |
| callHierarchy/outgoingCalls            |      |                                     |
| textDocument/moniker                   |      |                                     |

---

## Developing

`./gradlew runIde` to spawn a new Intellij session with the plugin.

or

`./gradlew buildPlugin` to build the plugin, then install it from disk in Intellij, the zip should be on `./build/distributions/*.zip`.


### NREPL

Unless you need to edit some generated extension file or kotlin file, mostly clojure code is editable via repl while your plugin is running.

NREPL is included in the plugin during development, so you can jack in and edit most of the plugin behavior while running it.

## Support the project

You can help us keep going and improving clojure-lsp-intellij by **[supporting the project](https://github.com/sponsors/clojure-lsp)**, the support helps to keep the project going and being updated and maintained.

