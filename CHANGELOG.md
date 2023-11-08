# Changelog

## [Unreleased]

## 0.13.3

- Fix exception during hover element.
- Fix exception during find definition specific cases.

## 0.13.2

- Fix support for older intellij.

## 0.13.1

- Fix classpath lookup injecting user env on default classpath lookup commands.

## 0.13.0

- Add support for paredit actions: slurp, barf, raise and kill.
- Bump clojure-lsp to `2023.10.30-16.25.41`.

## 0.12.5

- Allow specifying a server log-path to better troubleshooting.

## 0.12.4

- Fix find definition to work for external deps as well.

## 0.12.3

- Improve troubleshooting section

## 0.12.2

- Use nrepl and logger from clj4intellij.
- Improve line indent to recognize some macros.

## 0.12.1

- Fix exception on startup related to status bar.

## 0.12.0

- Implement lineIdentProvider to handle enters and move cursor to correct position.

## 0.11.2

- Start LSP server only when opening clojure files, avoiding starting on non Clojure projects. #20
- Fix LSP not being disconnected on project close/switch.

## 0.11.1

- Bump clj4intellij to `0.2.1`.

## 0.11.0

- Fix language attribute in intentionAction from plugin.xml. #18
- Fix documentationProvider plugin.xml. #19
- Extract Clojure intellij integration to separated lib clj4intellij.

## 0.10.1

- Fix Find definition to work with Ctrl+B + Ctrl+click.

## 0.10.0

- Add support for `textDocument/didSave` notification.
- Fix rename refactor when file is not opened.

## 0.9.0

- Avoid noisy exception after startup
- Add `textDocument/codeAction` support. #3

## 0.8.0

- Add support for refactorings via workspace/executeCommand. #4

## 0.7.0

- Add support for textDocument/rename feature. #6
- Add support for workspace/applyEdit. #7

## 0.6.0

- Add support for LSP notification window/showMessage and request window/showMessageRequest.
- Improve status bar to show icon instead of text.

## 0.5.0

- Add troubleshooting section to 'Tools > Clojure LSP'

## 0.4.0

- Add brace matcher for `[]`, `{}` and `()`
- Fix completion of items with `/`

## 0.3.0

- Add support for comments.
- Add support for quote handlers.
- Add support for completion. #2

## 0.2.0

- Support find defintion of external dependencies. #1

## 0.1.4

- Fix LSP startup messages to properly mention the task being done

## 0.1.3

- Require plugin restart after install because of Clojure load in Classloader.

## 0.1.2

- Support more intellij versions until 2021.3

## 0.1.1

- Improvements to plugin compatibility

## 0.1.0

- Support `initialize` and subsequent requests.
- Support `textDocument/didChange`, `textDocument/didClose`, `textDocument/didOpen`.
- Support `textDocument/hover`.
- Support `textDocument/references`.
- Support `textDocument/formatting`.
- Support `textDocument/codeLens` and `codeLens/resolve`.
- Add status bar with support for restarting server.
