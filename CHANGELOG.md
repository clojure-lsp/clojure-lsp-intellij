# Changelog

## [Unreleased]

- Bump clj4intellij to 0.7.1

## 3.1.1

- Remove `:` lexer check since this is delegated to clojure-lsp/clj-kondo already.

## 3.1.0

- Fix comment form complain about missing paren.
- Improve server installation fixing concurrency bugs + using lsp4ij install API.

## 3.0.2

- Fix Settings page exception when more than a project was opened and closed.

## 3.0.1

- Bump clj4intellij to 0.6.3.
- Fix code lens references not working when more than a project is opened. #67

## 3.0.0

- Fix brace matcher to insert closing brace for some missing cases.
- Add imcompatible tag with Cursive and Clojure-Kit plugins.
- Integrate with lsp4ij, a LSP client plugin, removing lots of logics from this plugin and fixing multiple bugs and issues. Fixes #63, #61, #59, #57, #53, #36, #21, #9, #5
- Drop support for older intellijs, supporting 2023.3 onwards.

## 2.6.4

- Fix shortcuts not being added after 2.6.3.

## 2.6.3
 
- Fix default shortcuts being added for already customized shortcuts.

## 2.6.2

- Basic support for form comments. #47

## 2.6.1

- Support starting plugin even with network issues to download latest server when one is already present. #56

## 2.6.0

- Support Create workspace edit operation. Fixes #51
- Support Rename workspace edit operation
- Support workspace/willRenameFiles allowing to rename ns, renaming the files properly.

## 2.5.0

- Add syntax highlighting to Clojure documentation codeblocks

## 2.4.5

- Only log server communication when trace-level matches.
- Improve find definition of external files to consider opened files, avoiding exceptions.

## 2.4.4

- Fix freeze after using paredit raise.

## 2.4.3

- Avoid exception when clojure-lsp-version file is not present by previous plugin versions.

## 2.4.2

- Avoid Intellij freezing when processing edits from server during commands.

## 2.4.1

- Check if downloaded server is on latest version otherwise re-download it.

## 2.4.0

- Support window/showDocument request, supporiting moving cursor after applying paredit command. #45

## 2.3.10

- Fix rename file exception. #48

## 2.3.9

- Fix completion of labels that start with non letters like `|`. #46
- Scroll to proper position after finding definition/reference. #44

## 2.3.8

- Fix exceptions introduced on 2.3.5 when opening multiple projects.

## 2.3.7

- Fix minor exceptions happening with recent intellij versions.

## 2.3.6

- Check for LSP connected when executing actions.

## 2.3.5

- Support IntelliJ 2024.1. #43

## 2.3.4

- Fix completion of things that start with non letters like `|`.

## 2.3.3

- Fix freeze on initializing on most macos. #41

## 2.3.2

- Fix race condition NPE when intellij starts slowly.

## 2.3.1

- Add common shortcuts to DragForward and DragBackward.

## 2.3.0

- Support multiple projects opened with the plugin. #37
- Fix Stackoverflow exception when renaming. #32

## 2.2.1

- Wait to check for client initialized to minor cpu usage improvmenet.

## 2.2.0

- Improve Find references/implementations to go directly to the usage if only one is found. #39

## 2.1.0

- Support "Find implementations" of defmultis/defprotocols. #31
- Fix commands, code actions not being applied after 2.0.0.
- Improve "find declaration or usages" to show popup for references.

## 2.0.3

- Fix only noisy codelens exception. #33

## 2.0.2

- Fix references for different URIs when finding references.

## 2.0.1

- Fix os type for macos non aarch64 when downloading clojure-lsp server.

## 2.0.0

- Use clojure-lsp externally instead of built-in since causes PATH issues sometimes. Fixes #25 and #26
- Fix multiple code lens for the same line. #29

## 1.14.10

- Bump clojure-lsp to `2024.03.01-11.37.51`.

## 1.14.9

- Fix some exceptions that can rarely occurr after startup.

## 1.14.8

- Fix exception when starting server related to previous version.

## 1.14.7

- Bump clojure-lsp to `2024.02.01-11.01.59`.

## 1.14.6

- Add shortcuts to backward slurp and barf.
- Add shortcut documentation to all features, check the features doc page.
- Fix Rename feature not being available for some cases.

## 1.14.5

- Fix ctrl/cmd + click going to definition automatically. #27

## 1.14.4

- Fix possible exception when calculating code lens.

## 1.14.3

- Start NREPL server only for development.

## 1.14.2

- Improve project creation wizard icons.

## 1.14.1

- Bump clojure-lsp to `2023.12.29-12.09.27`.

## 1.14.0

- Add wizard to create multiple Clojure types of projects directly via Intellij.

## 1.13.5

- Fix format for non clojure files. #28

## 1.13.4

- Bump clojure-lsp to 2023.10.30-16.25.41-hotfix2 to fix settings merge during startup.

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
