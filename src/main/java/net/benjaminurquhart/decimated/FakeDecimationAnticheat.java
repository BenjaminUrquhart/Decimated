package net.benjaminurquhart.decimated;

import com.boehmod.lib.utils.BoehModLogger;

import net.decimation.mod.client.managers.DecimationClientAnticheat;

public class FakeDecimationAnticheat extends DecimationClientAnticheat {

	@Override
	public boolean isCheating() {
		BoehModLogger.printLine(BoehModLogger.EnumLogType.ANTICHEAT, "Injected aniticheat instance called!");
		DecimationClientAnticheat.CHEAT_REASON = "Can't be caught cheating if the anticheat is broken - oh wait";
		return false;
	}
}
