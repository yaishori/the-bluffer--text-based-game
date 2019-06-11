package assignment3.server;

class TBGPFactory implements ServerProtocolFactory<TBGPMessage> {
	private String jsonFileName;
	public TBGPFactory(String jsonFileName) {
		this.jsonFileName = jsonFileName;
	
	}

	public ServerProtocol<TBGPMessage> create(TBGPData tbgpData){
		return new TBGP(tbgpData, jsonFileName);
	}
}