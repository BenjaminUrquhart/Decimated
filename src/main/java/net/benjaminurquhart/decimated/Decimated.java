package net.benjaminurquhart.decimated;

import com.boehmod.lib.utils.BoehModLogger;
import com.boehmod.lib.utils.BoehModLogger.EnumLogType;

import sun.misc.Unsafe;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

import net.decimation.mod.*;
//import net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating_Request;

import java.lang.reflect.*;
import java.util.List;
import java.util.stream.Collectors;

@Mod(modid=Decimated.MODID, version=Decimated.VERSION, name="Decimated", dependencies="required-after:deci")
public class Decimated {

	@Instance("decimated")
	public static Decimated instance;
	
	public static final String MODID = "decimated";
	public static final String VERSION = "0.0.1a";
	
	private static EnumLogType LOG_TYPE = EnumLogType.INITIALIZATION;
	
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
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		LOG_TYPE = EnumLogType.ANTICHEAT;
		List<ModContainer> otherMods = Loader.instance().getActiveModList()
														.stream()
														.filter(mod -> !mod.getModId().equals("deci") && !mod.getModId().equals("gvc"))
														.filter(mod -> !mod.getModId().equals("FML") && !mod.getModId().equals("Forge"))
														.filter(mod -> !mod.getModId().equals("mcp"))
														.collect(Collectors.toList());
		log("Done with mod initialization.");
		log("The following Forge mod(s) will be hidden from the anticheat check:");
		for(ModContainer mod : otherMods) {
			log(String.format("%s %s (ID: %s, File: %s)", mod.getName(), mod.getDisplayVersion(), mod.getModId(), mod.getSource().getName()));
		}
		try {
			Class<?> liteloaderClass = Class.forName("com.mumfrey.liteloader.core.LiteLoader");
			Object liteloader = liteloaderClass.getMethod("getInstance").invoke(null);
			
			Field containerHolderField = liteloaderClass.getDeclaredField("mods");
			containerHolderField.setAccessible(true);
			Object holder = containerHolderField.get(liteloader);
			
			List<?> mods = (List<?>)holder.getClass().getMethod("getLoadedMods").invoke(holder);
			
			Class<?> modInfoClass = Class.forName("com.mumfrey.liteloader.core.ModInfo");
			
			Method name = null, version = null, id = null, urlMethod = null;
			String url;
			log("The following litemod(s) will be hidden from the anticheat check:");
			for(Object mod : mods) {
				mod = modInfoClass.cast(mod);
				if(name == null) {
					name = modInfoClass.getMethod("getDisplayName");
					name.setAccessible(true);
				}
				if(version == null) {
					version = modInfoClass.getMethod("getVersion");
					name.setAccessible(true);
				}
				if(id == null) {
					id = modInfoClass.getMethod("getIdentifier");
					id.setAccessible(true);
				}
				if(urlMethod == null) {
					urlMethod = modInfoClass.getMethod("getURL");
					urlMethod.setAccessible(true);
				}
				url = String.valueOf(urlMethod.invoke(mod));
				if(url.endsWith("/")) {
					url = url.substring(0, url.length() - 1);
				}
				if(url.contains("/")) {
					url = url.substring(url.lastIndexOf("/") + 1);
				}
				if(url.trim().isEmpty()) {
					url = "<unknown file>";
				}
				log(String.format("%s %s (ID: %s, File: %s)", name.invoke(mod), version.invoke(mod), id.invoke(mod), url));
			}
		}
		catch(ClassNotFoundException e) {
			log("Liteloader is not installed. Skipping litemod check (" + e + ")");
		} 
		catch (Exception e) {
			err("An error occurred when getting litemods:");
			e.printStackTrace();
		}
	}
	private void makeUnfinal(Field field) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field modifiers = Field.class.getDeclaredField("modifiers");
		modifiers.setAccessible(true);
		modifiers.setInt(field, modifiers.getInt(field) & ~Modifier.FINAL);
	}
	protected static void log(String s) {
		BoehModLogger.printLine(LOG_TYPE, s);
	}
	protected static void err(String s) {
		BoehModLogger.printError(LOG_TYPE, s);
	}
}
