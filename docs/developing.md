# Developing

`./gradlew runIde` to spawn a new Intellij session with the plugin.

or

`bb install-plugin` to build and install the plugin.

## NREPL

Unless you need to edit some generated extension file or kotlin file, mostly clojure code is editable via repl while your plugin is running!

NREPL is included in the plugin during development, so you can jack in and edit most of the plugin behavior while running it.
