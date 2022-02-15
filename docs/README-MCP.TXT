Minecraft mod creator pack 4.3 for Minecraft 1.7.3
==================================================

Prerequisites:
==============
1.) Install Java SDK Standard Edition (short JDK). 
    Link: http://www.oracle.com/technetwork/java/javase/downloads/

2.) Add the paths to your JDK and JRE bin folders to the Environment Variable PATH. 

    Description where to find the variable:
    http://www.java.com/en/download/help/path.xml

    Example for Windows users of what you have to add to the variable (entries are seperated by ";" )
    C:\Program Files\Java\jdk1.6.0_24\bin;C:\Program Files\Java\jre6\bin

    We wont provide any help setting it up - this _is_ a prerequesite.

How to use:
===========
1) Prepare the files:
- Copy the "minecraft_server.jar" file into the "jars" folder.
- Copy the folders "bin" and "resources" from your "%APPDATA$\.minecraft" folder into the "jars" folder.

2) Decompilation and patching
- Start the "decompile.bat" script in this folder.

3) Modding
- Modify the sourcecode in the "src\minecraft" folder or in the "src\minecraft_server" folder.

4) Compile
- Start the "recompile.bat" script in this folder.

5) Testing
- To test the modified game, start the "startclient.bat" script
- To test the modified server, start the "startserver.bat" script

6) Obfuscation
- Decompile the code, modify and recompile.
- Start "reobfuscate.bat" to start the reobfuscation step, it will automatically detect changed classes and reobfuscate them.
- Your obfuscated classes are now available in "reobf\minecraft" and "reobf\minecraft_server", ready to be injected in MC.
- Make sure to delete the META-INF folder in minecraft.jar, otherwise the game will just black-screen when you start it.

WARNINGS:
=========
- Make sure that you backup the modified sources before you run "decompile.bat" again or all changes will be lost!
- The "cleanup.bat" file will delete most of the generated files and sources. Be careful with this one :)

Notes:
======
* If you want to include external libraries (either as .jar or .class) to the classpath during recompilation add them
  to a folder called "lib" creating it first if required.

* Should mcp complain about "java.lang.NoClassDefFoundError: Start" when trying to run startclient.bat or startserver.bat
  try cleanup.bat (make backups of you sources!), decompile again and recompile as the final step.

* Do not use this to release complete packages of minecraft jar, class or java files. They are copyrighted
  material by Notch and mods should only contain small changes to some classes, never complete sets that
  can be used by people who did not buy the game to play it.

* Make sure you use the original minecraft.jar and minecraft_server.jar files. If you have already modded them
  they will NOT work with the patches in these scripts.

* The "startclient.bat" file uses the "Start.class" file to start the game. This will make sure the game will not
  use your "%APPDATA%\.minecraft" folder, but instead use the "jars" folder for all saves. So any bugs in the modified
  game will not corrupt your normal worlds.

* If you have any problems using this toolpack, put the "logs\*.log" files that the scripts generated into a
  zip-file and send it to us (post it in the minecraft forum):
  http://www.minecraftforum.net/viewtopic.php?f=25&t=58464
  
* This version of the mod creator package uses a deobfuscator to change all field and method names in the sources.
  Look in the csv files in "conf", namely "conf\classes.csv", "conf\fields.csv" and "conf\methods.csv" files for a complete mapping of the names.

* There are currently no known bugs in the recompiled game or server, except those that were already in the original
  game :)

* If your reobfuscated classes cause a black screen in Minecraft, make sure that you've deleted the META-INF folder
  in the minecraft.jar file.

* To add new resources to the game, for example png files, put them in the bin folder tree, they will also get copied to the reob folder
  automatically when you reobfuscate your changes.

Credits:
========
Searge
* Creator of MCP
* Fixes all compile errors in the decompiled sourcecode
* Created the MCP Mod Framework system and API
* Created the new RetroGuard deobfuscation module for MCP 3.x
* Created the Exceptor tool for MCP 4.x
* Created the Eclipse workspace for MCP 4.x
ProfMobius
* Creator of the renaming codes and re-obfuscation procedures
* Helped to port scripts to Linux
* Developer and maintainer of the MCP chan bot
* Is now bald after working too much with java constant pool and re-obfuscation
* Created the new workflow scripts and renamer for MCP 3.0
IngisKahn
* Creator of the bytecode compare tool that helps us to update the name mappings quickly for new minecraft versions
* Contributed to the de-obfuscation spreadsheet
* Working hard on creating better internal tools for mapping updates and decompiling
Fesh0r
* php/sql code monkey
* Uses his magic to create the mappings, patches, and general release work
* Has Searge's approval to make official MCP releases ;)
* Makes sure we get proper patches for the sourcecode that JAD generates
ZeuX
* Helps out in the IRC channels - Head of HR
* Did server patches for the most recent versions - if you run into any (patch-related) problems, it's his fault :P
303
* Wiki contributor
* Tries to help out newbies in the IRC channels
* Created some scripts for mod loader support
Mr_okushama
* Wiki contributor
* Public Support Manager
* IRC Operator
* Savior of the 2011 April fools prank
Generic
* Works on improving IngisKahn's bytecode compare tool
* Added some important features to retroguard
Risugami
* The guy who created the first mods I (Searge) ever used in Minecraft
* The creator of modloader who gave us permission to include files from his system in MCP
fotoply
* Helped to improve the batch files
ScottyDoesKnow
* obfuscathonCharmer, the obfuscathon GUI
Cadde
* Community manager and Wiki manager
* Works on the de-obfuscation spreadsheet
* Mod support (making old mods work with MCP)
* All round handyman
Vaprtek
* Works on the de-obfuscation spreadsheet
* Knows how to make pet creepers
gronk
* Script support
n00bish
* Linux script maintenance
Sage Pourpre
* His thread in the forums inspired me (Searge) to create this toolpack in the first place
Tei
* Supported the MCP project since the first version was released
spec10
* The new linux scripts guy
Head
* Wiki contributor / Administrator
* Explains classes and their members on the Wiki
MissLil
* Various scripting stuff
* Lots of reverse engineering
* OpenGL constants annoting
Chase
* MCP Launcher Work
* External jar loading
* Scrollable mod list.

and of course:
- Everybody who contributed to the great google spreadsheet or who created some mods (I've got them all :).
- NOTCH for creating a game that is just awesome, I hope he does not feel offended by our decompiling efforts.
        Please, Notch, support our ambitions to mod your game. I know people who bought it just because of
        some great mods.

History:
========
4.3  - Updated to support Minecraft 1.7.3 and MinecraftServer 1.7.3
4.2  - Updated to support Minecraft 1.7.2 and MinecraftServer 1.7.2
4.1  - Added an Eclipse workspace and included a reobfuscation bugfix
4.0  - Added improved decompile scripts with new Exceptor tool
3.4  - Updated to support Minecraft 1.6.6 and MinecraftServer 1.6.6
3.3  - Updated to support Minecraft 1.6.5 and MinecraftServer 1.6.5
3.2  - Updated to support Minecraft 1.6.4 and MinecraftServer 1.6.4
3.1  - Complete rewrite of the framework, works with Minecraft 1.5_01 and MinecraftServer 1.5_02
2.12 - Updated to support Minecraft 1.5_01 and MinecraftServer 1.5_02
2.11 - Updated to support Minecraft 1.4_01 and MinecraftServer 1.4_01
2.10 - Updated to support Minecraft 1.4 and MinecraftServer 1.4
2.9a - Added MCP Mod System for 1.3_01, added mod loader support, updated mappings
2.9  - Updated to support Minecraft 1.3_01 and MinecraftServer 1.3
2.8  - Added the MCP mod system SDK and support for OSX
2.7  - Updated to support Minecraft 1.2_02 and MinecraftServer 1.2_01
2.6  - Updated to support Minecraft 1.1_02 and MinecraftServer 1.1_02
2.5  - Updated to support Minecraft 1.2.6 and MinecraftServer 0.2.8
2.4  - Updated to support Minecraft 1.2.5 and MinecraftServer 0.2.7
2.3  - Updated to support Minecraft 1.2.3_04 and MinecraftServer 0.2.5_02. Linux support beta.
2.2a - Bugfix release to improve the re-obfuscation tools
2.2  - The reobfuscation beta test release. Still for Minecraft 1.2.2
2.1  - Updated to support Minecraft 1.2.2
2.0a - Bugfix release
2.0  - Major updates to MCP and support for post-Halloween versions of Minecraft
1.6  - All classes have meaningful names now, the class name mappings and the field name mappings are applied
1.5  - Extend the scripts to also support decompiling, recompiling and testing the minecraft_server.jar file
1.4  - Using a deobfuscator to rename all fields and methods and jadretro to fix some decompile bugs
1.3  - Added upgrade scripts to decompile and recompile Minecraft.class, MinecraftApplet.class and MinecraftServer.class
1.2  - Redirect output of all tools to a logfile
1.1  - Fixed TNT bug
1.0  - First release

Roadmap:
========
4.4+ - New awesome features, improvements and updates :)

_________________________________________________________________________________________________________________
_________________________________________________________________________________________________________________
________,.--:=r=.._______________________________________________________________________________,,...___________
_ -:'`      :^)-.!..[``P:  ___________________________________________________________  ,.=~r?`[:_i=.    `'~-._ _
_ .         :=)~.|:.|  [j  ___________________________________________________________  l|  [3 .:,[:.         ! _
_   :       :.[ .|:.(  [| ______________________________________________________________[|=^): [_ | .         ! _
__          :~)=.i..j  []  ___________________________________________________________  )|:')  [ _|;.         ! _
_ .         :.)..i._|;.)|  ___________________________________________________________  [!:=[` |: | .         ! _
_ )   _     :.[ .!  | .[|  ___________________________________________________________  )j:^[` (: | .         ! _
_   :_      <=l:.____ . |  ___________________________________________________________  [!:``   :.|:.       . ! _
_ ._ _ __ __:..;.|,.) .[j  ___________________________________________________________  [!3`T``l:.  .      `  ! _
____ [,...-+=c=f7l`!-( _____.~~~~~~~~._____=~~~~~~~/_____,>====~<.y=.__y~~~~~~~~~~=~.._ ```[j)=[=[.;,,_   _|'`` _
____ [ -~= ( '=r::::~( _____!<.      L____F      .=._ ,c   ;>~~<.   [_ F<.   .>~~<.   ".__ (.......!.( ..  | ____
____ [`) [ !   !^) !') ______ [ JL   (___F Z.   ( ___/   ./ ____ ". J___ L   ? ___J.   J _ |-l ,.(   !-| )-| ____
____ ) !`    (  ```[ ( _____ :. [[   J__F /]   .L___F   .F _______"L L__J    [____/    / _ | )~~~~ r-  !~! | ____
____ [ . . [_) (___|_( _____ | :L!   J.F / L   J __[    | _________   _ [   =+~~=l   ;/ __ |'[^^^( [ :'. ::( ____
____ [..     (.;.i.) ( ______L ? :.   * / J.   [__ L    L______________:L   ;...=+=^` ____ (`["[ l [`` ! ! | ____
____ )..   ....  !.. ( _____J. L_ L    / _[   :L__ [    [_________,.__ |   .L_____________ (   .       !   | ____
____ )..   --. --..!-( _____[ :.__|   / _.L   J ___"c   ?.______,/.;F _L   J._____________ [_[___  __     _| ____
____ )~)~r  -)~r - ( ( __:tT   ":LJ  / r^`     ")___ \.   7*==^`.;* r^`     "tL___________ ! (   ,,[   ._].|_____
___ _z;! : = ( ( : !=( __'^^^^^^"  ^^  *^^^^^^^^`____   "*~~~~^`  __"^^^^^^^^" ___________ ;.)   ( ( ..!.!.| ____
____F"*:P*si_(^^======.____________________________________________________________________j.....[.( !,.ztSP\ ___
_ _!      7*.)      L L___________________________________________________________________: J      :?x>'`    l___
__[ ;~.! ; :=l .-<--~~[___________________________________________________________________|,J  .__ !. :.  _,_ . _
 I :`~.  `\[ (.J-=    [___________________________________________________________________|    ._L_)~\./  ..==`. 
-.;`'=._ .L!;)-+--=-=-l___________________________________________________________________|___;__[_;=[\    ,>=`  
_   =-: `` _ --:...-...___________________________________________________________________  :_:  ._[   \=^  .-!  
_  ` -  L______-.-  ..-___________________________________________________________________: !    ._____ -' _ -' _
_____    _________________________________________________________________________________        ______ '  _____


    d8b   db  .d88b.  db   d8b   db   d888888b d888888b   d888888b .d8888. 
    888o  88 .8P  Y8. 88   I8I   88     `88'   `~~88~~'     `88'   88'  YP 
    88V8o 88 88    88 88   I8I   88      88       88         88    `8bo.   
    88 V8o88 88    88 Y8   I8I   88      88       88         88      `Y8b. 
    88  V888 `8b  d8' `8b d8'8b d8'     .88.      88        .88.   db   8D 
    VP   V8P  `Y88P'   `8b8' `8d8'    Y888888P    YP      Y888888P `8888Y' 
                                                                           
                                                                           
  db    db  .d88b.  db    db d8888b.  .o88b. d8888b.  .d8b.  d88888b d888888b 
  `8b  d8' .8P  Y8. 88    88 88  `8D d8P  Y8 88  `8D d8' `8b 88'     `~~88~~' 
   `8bd8'  88    88 88    88 88oobY' 8P      88oobY' 88ooo88 88ooo      88    
     88    88    88 88    88 88`8b   8b      88`8b   88~~~88 88~~~      88    
     88    `8b  d8' 88b  d88 88 `88. Y8b  d8 88 `88. 88   88 88         88    
     YP     `Y88P'  ~Y8888P' 88   YD  `Y88P' 88   YD YP   YP YP         YP 

======================================================================================================================
