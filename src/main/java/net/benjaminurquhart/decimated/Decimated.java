package net.benjaminurquhart.decimated;

import com.boehmod.lib.utils.BoehModLogger;
import com.boehmod.lib.utils.BoehModLogger.EnumLogType;

import sun.misc.Unsafe;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.EventHandler;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import net.decimation.mod.*;
//import net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating_Request;

import java.lang.reflect.*;

@Mod(modid=Decimated.MODID, version=Decimated.VERSION, name="Decimated", dependencies="required-after:deci")
public class Decimated {

	@Instance("decimated")
	public static Decimated instance;
	
	public static final String MODID = "decimated";
	public static final String VERSION = "0.0.1a";
	
	@SuppressWarnings("restriction")
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		ProxyCommon proxy = Decimation.instance.getProxy();
		if(proxy instanceof ProxyServer) {
			err("This is a server environment!");
			return;
		}
		ProxyClient client = (ProxyClient)proxy;
		FakeDecimationAnticheat fake = new FakeDecimationAnticheat();
		log("Attempting to inject fake anticheat instance...");
		try {
			Field field = client.getClass().getDeclaredField("decimationClientAnticheat");
			field.setAccessible(true);
			makeUnfinal(field);
			field.set(client, fake);
			log("Done injecting! Verifying...");
			if(client.getAnticheat() instanceof FakeDecimationAnticheat) {
				log("Verified! Class name is " + client.getAnticheat().getClass().getName());
			}
			else {
				err("Failed!");
				err("Expected: " + fake.getClass().getName());
				err("Found:    " + client.getAnticheat().getClass().getName());
			}
		}
		catch(Exception e) {
			err("An error occured!");
			e.printStackTrace();
		}
		Decimation deci = Decimation.getDecimation();
		log("Attempting to inject fake network wrapper...");
		try {
			Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			Unsafe unsafe = (Unsafe) unsafeField.get(null);
			
			FakeNetworkWrapper wrapper = (FakeNetworkWrapper) unsafe.allocateInstance(FakeNetworkWrapper.class);
			wrapper.setChannel(deci.getPacketChannel());
			
			Field field = deci.getClass().getDeclaredField("decimationPacketChannel");
			field.setAccessible(true);
			makeUnfinal(field);
			field.set(deci, wrapper);
			log("Done injecting! Verifying...");
			
			if(deci.getPacketChannel() instanceof FakeNetworkWrapper) {
				log("Verified! Class name is " + deci.getPacketChannel().getClass().getName());
			}
			else {
				err("Failed!");
				err("Expected: " + wrapper.getClass().getName());
				err("Found:    " + deci.getPacketChannel().getClass().getName());
			}
		}
		catch(Exception e) {
			err("An error occured!");
			e.printStackTrace();
		}
		/*
		Class<Message_Cheating_Request> clazz = Message_Cheating_Request.class;
		log("Patching " + clazz.getName() + " (even though it's the wrong class)...");
		
		try {
			DecimationAnticheatPatcher.patch();
			log("Patched successfully");
		}
		catch(Throwable e) {
			err("An error occured!");
			e.printStackTrace();
		}*/
	}
	private void makeUnfinal(Field field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(field, modifiers.getInt(field) & ~Modifier.FINAL);
	}
	private void log(String s) {
		BoehModLogger.printLine(EnumLogType.INITIALIZATION, s);
	}
	private void err(String s) {
		BoehModLogger.printError(EnumLogType.INITIALIZATION, s);
	}
}
