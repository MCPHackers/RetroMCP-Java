# RetroMCP-Java

RetroMCP is a modification of the Minecraft Coder Pack to create a "Long Term Service" patch for Minecraft.
RetroMCP-Java is a complete re-design of RetroMCP in Java.

# Using

Using RetroMCP-Java is simple!
1. Download and install JDK 8. MCPHackers recommend [Azul Zulu](https://www.azul.com/downloads/?version=java-8-lts&package=jdk).
2. Run the latest [release](https://github.com/MCPHackers/RetroMCP-Java/releases) from the command line or via double click. If you run it via double click and RMCP errors, make sure your PATH
and your JAR file associations are properly configured.
	> Be careful! Using "Open with" context menu on Windows will not use a proper directory, be sure to change the default .jar file associations
3. Run `setup` and choose the version you wish to decompile.
4. Run the `decompile` task
5. Mod away! Now it's Yourcraft!

For more information, check out the [RetroMCP Wiki](https://github.com/MCPHackers/RetroMCP-Java/wiki).

# Features

* Automatically download Minecraft .jar and libraries from version JSONs
* An improved launch method using [LaunchWrapper](https://github.com/MCPHackers/LaunchWrapper)
* Reobfuscation for all available versions
* Automatic IDE project creation for Eclipse, Intellij, and Visual Studio Code
* Merged codebase generation

# Building

1. Use a Git client or download the sources as a zip.
    > `git clone git@github.com:MCPHackers/RetroMCP-Java.git`
2. Switch to the repository folder.
    > `cd RetroMCP-Java`
3. Invoke the build task using Gradle or the Gradle wrapper.
    > `gradlew build`

# Contributing

If you encounter any issues or bugs with RetroMCP, please create an issue and explain it in detail!<br>
If you want to contribute, please keep pull requests about one topic instead of one huge pull request.<br>
We thank everyone who contributes to this project!
