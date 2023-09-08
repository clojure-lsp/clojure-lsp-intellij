# Changelog

## [Unreleased]

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
