# clojure-lsp-intellij

## IN DEVELOPMENT

This plugin is still in development and it's not expected to be functional yet, when in a usable stage we should release alpha versions.

Also, it doesn't use IntelliJ's LSP support for 2 reasons:
  - The LSP feature is only available for Ultimate Edition (paid edition), making this plugin only available for those editions
  - The LSP feature is pretty alpha, missing some features yet, some that were already implemented in this plugin.

---

<!-- Plugin description -->

Plugin with support for Clojure & ClojureScript development via Language Server (LSP)

<!-- Plugin description end -->

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


## NREPL

Unless you need to edit some generated extension or java class, mostly clojure code is editable via repl while your plugin is running.

NREPL is included in the plugin during development, so you can jack in and edit most of the plugin behavior while running it.
