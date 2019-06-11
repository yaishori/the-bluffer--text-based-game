package assignment3.server.reactor;

import assignment3.server.TBGP;
import assignment3.server.TBGPData;
import assignment3.server.TBGPMessage;

public class AsyncTBGP extends TBGP implements AsyncServerProtocol<TBGPMessage> {

	public AsyncTBGP(TBGPData tbgpData, String jsonFileName) {
		super(tbgpData, jsonFileName);
	}

	@Override
	public boolean shouldClose() {
		return hasQuitted;
	}

	@Override
	public void connectionTerminated() {
		isConnectionTerminated = true;
	}
}
