<img src="images/logo-dark.svg" width="180" align="right">

[![JetBrains Plugin Version](https://img.shields.io/jetbrains/plugin/v/com.github.clojure-lsp?style=flat-square&labelColor=91B6FB&color=93DA52&link=https%3A%2F%2Fplugins.jetbrains.com%2Fplugin%2F22489-clojure-lsp)](https://plugins.jetbrains.com/plugin/22489-clojure-lsp)
[![Slack community](https://img.shields.io/badge/Slack-chat-blue?style=flat-square&labelColor=91B6FB&color=93DA52)](https://clojurians.slack.com/archives/CPABC1H61)

# clojure-lsp-intellij

<!-- Plugin description -->

[Free OpenSource Intellij plugin](https://github.com/clojure-lsp/clojure-lsp-intellij) with support for Clojure & ClojureScript development using the Language Server (LSP) [clojure-lsp](https://clojure-lsp.io/) providing features via static analysis

<!-- Plugin description end -->

![Clojure LSP Intellij](images/demo.png)

---

<p align="center">
  <a href="https://clojure-lsp.io"><strong>homepage</strong></a> •
  <a href="docs/features.md"><strong>features</strong></a> •
  <a href="docs/developing.md"><strong>developing</strong></a> •
  <!-- <a href="https://clojure-lsp.io/settings"><strong>settings</strong></a> • -->
  <a href="docs/troubleshooting.md"><strong>troubleshooting</strong></a> •
  <a href="https://github.com/sponsors/clojure-lsp"><strong>support us</strong></a>
</p>
<hr>

## Rationale

IntelliJ is the only mainstream editor without a plugin with good, free, and dedicated support for LSP, there are already excellent plugins for Clojure like [Cursive](https://cursive-ide.com/) which provides lots of features with REPL support or [ClojureKit](https://github.com/gregsh/Clojure-Kit) which adds basic Clojure support for the language, but none uses clojure-lsp or follows the LSP standard which some users may prefer as [some features](https://clojure-lsp.io/features/) are only available in clojure-lsp.

Keep in mind that this plugin provides only LSP features relying on clojure-lsp (and clj-kondo under the hood) static analysis, so no runtime features exist, like REPL integration or support, for that a separated plugin like [clojure-repl-intellij](https://github.com/afucher/clojure-repl-intellij) is recommended as it's not possible to use only the REPL part of other plugins like Cursive together with this plugin.

Also, this plugin does not use official IntelliJ's LSP support yet for 2 reasons:
  - It's only available for Ultimate Edition (paid edition), making this plugin only available for those editions.
  - It's pretty alpha, missing some features yet, some that were already implemented in this plugin.

---

## Getting Started

When opening a Clojure project or file, the plugin will try to download latest clojure-lsp server or use it from a custom path if specified in IntelliJ settings, then it will spawn the process for each project and communicate with it to provide [all features](https://clojure-lsp.io/features/).

Make sure to check the [Clojure REPL plugin](https://github.com/afucher/clojure-repl-intellij/) too.

## Contributing

Contributions are very welcome, check the [issues page](https://github.com/clojure-lsp/clojure-lsp-intellij/issues) for more information about what are good first issues or open an issue describing the desired support.

## Support the project 💙

You can help us keep going and improving clojure-lsp-intellij by **[supporting the project](https://github.com/sponsors/clojure-lsp)**, the support helps to keep the project going, updated, and maintained.
