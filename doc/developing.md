# Developing

`./gradlew runIde` to spawn a new Intellij session with the plugin.

or

`./gradlew buildPlugin` to build the plugin, then install it from disk in Intellij, the zip should be on `./build/distributions/*.zip`.

## NREPL

Unless you need to edit some generated extension file or kotlin file, mostly clojure code is editable via repl while your plugin is running!

NREPL is included in the plugin during development, so you can jack in and edit most of the plugin behavior while running it.
