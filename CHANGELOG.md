# Changelog

## [Unreleased]

- Add brace matcher for `[]`, `{}` and `()`.

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
