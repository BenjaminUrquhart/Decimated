# Decimated
Exploits weaknesses in the Decimation anticheat system to allow loading of arbitrary mods.

Special thanks to [Natan](https://github.com/natanbc) for creating the (unused) bytecode patcher.

[Demo](https://youtu.be/jHSW7JgRFNE)

# How does it work?

First, you need to know how the anticheat system works.

The system is split into 2 parts:

**net.decimation.mod.client.managers.DecimationClientAnticheat**
Checks:
- Loaded mods
- Number of files in the `mods` folder
- If liteloader is present
- Whether Cheat Engine or Process Hacker is running

**net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating_Request.Handler**
Checks:
- Total size of all files in the `mods` folder

All seems lost... until we realize that the `DecimationClientAnticheat` class is *not* final. That means [we can provide our own dummy implementation](https://github.com/BenjaminUrquhart/Decimated/blob/master/src/main/java/net/benjaminurquhart/decimated/FakeDecimationAnticheat.java), removing most of the annoying checks.

The next hurdle is `Message_Cheating_Request.Handler`. Since it compares the size of the mod folder with that of the server, we can't put any additional files in there. Or can we...

Let's look at the implementation of this class:
```java
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(Message_Cheating_Request message, MessageContext ctx) {
            EntityPlayer player = Minecraft.func_71410_x().field_71439_g;
            if (player.func_145782_y() == message.entityID || message.entityID == 0) {
                File folder = new File("mods/");
                File[] listOfFiles = folder.listFiles();
                long myDirectorySize = 0L;
                File[] var8 = listOfFiles;
                int var9 = listOfFiles.length;

                for(int var10 = 0; var10 < var9; ++var10) {
                    File file = var8[var10];
                    if (file.isFile() && !file.getAbsoluteFile().getName().toLowerCase().contains("optifine")) {
                        myDirectorySize += file.getAbsoluteFile().length();
                    }
                }

                Decimation.getDecimation().getPacketChannel().sendToServer(new Message_Cheating(VariablesClient.cheating, myDirectorySize));
            }

            return null;
        }
```
Notice something?

This class ignores files that have the word "optifine" in their name. This means we can simply rename mod jars to contain "optifine" and the anticheat will ignore them. Even better, this check ignores subdirectories. If we have liteloader enabled (remember, we already removed the check for it), we can place all our mods in the `1.7.10` subdirectory without needing to rename anything.

**Development**

Turns out you can just ignore the server's cheat request packet. Go figure. I've opted to instead [fake the responses](https://github.com/BenjaminUrquhart/Decimated/blob/master/src/main/java/net/benjaminurquhart/decimated/FakeNetworkWrapper.java#L40) since not responding is easily detectable. 

# Solutions
There are none. You can make `DecimationClientAnticheat` final and make `Message_Cheating_Request.Handler` check subdirectories, but we can just use bytecode patching to override the checks. The only way to prevent the patching is to implement the entire system using native code, which is infeasable.

# Update
Shortly after I created this, Scott started obfuscating the decimation builds and completely refactored the anticheat system. The result is the code in its current state does NOT work and will likely crash your game during preinit. Because of its obsolence, I've decided to make this repository public as it is technically no longer an exploit.
