package net.benjaminurquhart.decimated;

import com.boehmod.lib.utils.BoehModLogger;
import com.boehmod.lib.utils.BoehModLogger.EnumLogType;

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
    		BoehModLogger.printLine(EnumLogType.ANTICHEAT, "Dropped cheating response (isCheating=" + msg.isCheating + ", directorySize=" + msg.directorySize + ")");
    		return;
    	}
    	real.sendToServer(message);
    }
}
