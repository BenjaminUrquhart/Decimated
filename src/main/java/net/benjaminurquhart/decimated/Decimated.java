package net.benjaminurquhart.decimated;

import com.boehmod.lib.utils.BoehModLogger;
import com.boehmod.lib.utils.BoehModLogger.EnumLogType;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.EventHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import net.decimation.mod.*;
import net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating_Request;

import java.lang.reflect.*;

@Mod(modid=Decimated.MODID, version=Decimated.VERSION, name="Decimated", dependencies="required-after:deci")
public class Decimated {

	@Instance("decimated")
	public static Decimated instance;
	
	public static final String MODID = "decimated";
	public static final String VERSION = "0.0.1a";
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ProxyCommon proxy = Decimation.instance.getProxy();
		if(proxy instanceof ProxyServer) {
			BoehModLogger.printError(EnumLogType.INITIALIZATION, "This is a server environment!");
			return;
		}
		ProxyClient client = (ProxyClient)proxy;
		FakeDecimationAnticheat fake = new FakeDecimationAnticheat();
		BoehModLogger.printLine(EnumLogType.INITIALIZATION, "Attempting to inject fake anticheat instance...");
		try {
			Field field = client.getClass().getDeclaredField("decimationClientAnticheat");
			field.setAccessible(true);
			
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, modifiers.getInt(field) & ~Modifier.FINAL);
			
			field.set(client, fake);
			BoehModLogger.printLine(EnumLogType.INITIALIZATION, "Done injecting! Verifying...");
			if(client.getAnticheat() instanceof FakeDecimationAnticheat) {
				BoehModLogger.printLine(EnumLogType.INITIALIZATION, "Verified! Class name is " + client.getAnticheat().getClass().getName());
			}
			else {
				BoehModLogger.printError(EnumLogType.INITIALIZATION, "Failed!");
				BoehModLogger.printError(EnumLogType.INITIALIZATION, "Expected: " + fake.getClass().getName());
				BoehModLogger.printError(EnumLogType.INITIALIZATION, "Found:    " + client.getAnticheat().getClass().getName());
			}
		}
		catch(Exception e) {
			BoehModLogger.printError(EnumLogType.INITIALIZATION, "An error occured!");
			e.printStackTrace();
		}
		Class<Message_Cheating_Request> clazz = Message_Cheating_Request.class;
		BoehModLogger.printLine(EnumLogType.INITIALIZATION, "Patching " + clazz.getName() + " (even though it's the wrong class)...");
		
		try {
			DecimationAnticheatPatcher.patch();
			BoehModLogger.printLine(EnumLogType.INITIALIZATION, "Patched successfully");
		}
		catch(Throwable e) {
			BoehModLogger.printError(EnumLogType.INITIALIZATION, "An error occured!");
			e.printStackTrace();
		}
	}
}
