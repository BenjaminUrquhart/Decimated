package net.benjaminurquhart.decimated;

import java.io.File;
import java.util.List;

import com.boehmod.lib.utils.BoehModLogger;
import com.boehmod.lib.utils.BoehModLogger.EnumLogType;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.decimation.mod.utilities.net.messages_minecraft.Message_Cheating;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.Packet;

public class FakeNetworkWrapper extends SimpleNetworkWrapper {

	private SimpleNetworkWrapper real;
	
	public FakeNetworkWrapper() {
		super("ctx");
	}
	
	protected void setChannel(SimpleNetworkWrapper channel) {
		this.real = channel;
	}
	@Override
    public Packet getPacketFrom(IMessage message) {
        return real.getPacketFrom(message);
    }
	@Override
    public void sendToAll(IMessage message) {
    	real.sendToAll(message);
    }
	@Override
    public void sendTo(IMessage message, EntityPlayerMP player) {
    	real.sendTo(message, player);
    }
	
	@Override
    public void sendToServer(IMessage message) {
    	if(message instanceof Message_Cheating) {
    		Message_Cheating msg = (Message_Cheating) message;
    		BoehModLogger.printLine(EnumLogType.ANTICHEAT, "Intercepted cheating response");
    		BoehModLogger.printLine(EnumLogType.ANTICHEAT, "Cheating: " + msg.isCheating);
    		BoehModLogger.printLine(EnumLogType.ANTICHEAT, "Directory size: " + msg.directorySize);
    		msg.isCheating = false;
    		msg.directorySize = calculateSize();
    		BoehModLogger.printLine(EnumLogType.ANTICHEAT, "New Directory size: " + msg.directorySize);
    	}
    	real.sendToServer(message);
    }
	private long calculateSize() {
		List<ModContainer> mods = Loader.instance().getActiveModList();
		long size = mods.stream()
						.filter(mod -> mod.getModId().equals("deci") || mod.getModId().equals("gvc"))
						.map(ModContainer::getSource)
						.peek(file -> BoehModLogger.printLine(EnumLogType.ANTICHEAT, "Found whitelisted mod in " + file.getName() + " (Size: " + file.length() + ")"))
						.mapToLong(File::length)
						.sum();
		return size;
	}
}
