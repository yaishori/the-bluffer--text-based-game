package assignment3.server.reactor;

import assignment3.server.TBGPData;
import assignment3.server.TBGPMessage;

class AsyncTBGPFactory implements AsyncServerProtocolFactory<TBGPMessage> {
	private String jsonFileName;
	public AsyncTBGPFactory(String jsonFileName) {
		this.jsonFileName = jsonFileName;
	
	}

	public AsyncServerProtocol<TBGPMessage> create(TBGPData tbgpData){
		return new AsyncTBGP(tbgpData, jsonFileName);
	}
}