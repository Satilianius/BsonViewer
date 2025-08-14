# BsonViewer

![Build](https://github.com/Satilianius/BsonViewer/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/28119-bsonviewer.svg)](https://plugins.jetbrains.com/plugin/28119-bsonviewer)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/28119-bsonviewer.svg)](https://plugins.jetbrains.com/plugin/28119-bsonviewer)

<!-- Plugin description -->

BsonViewer allows you to view and edit MongoDB BSON files as JSON files directly in the editor window.

## Features:
- Open and edit BSON files directly in your IDE
- Automatic conversion between BSON and JSON formats
- Validation to ensure data integrity
- Save modifications back to BSON format

## Limitations:
- Only valid JSON file can be converted to BSON, so if after editing the file contains invalid JSON, the last valid version will be saved instead
- BSON format does not preserve formatting, so the default Jackson prettyPrinter is always used to format the file
- Undo and Redo actions are not supported yet

## Support
Found an issue? Have a feature request? Visit [GitHub repository](https://github.com/Satilianius/BsonViewer/issues) or [contact the developer](mailto:satilianius@gmail.com).

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "BsonViewer"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/28119-bsonviewer) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/28119-bsonviewer/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Satilianius/BsonViewer/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
