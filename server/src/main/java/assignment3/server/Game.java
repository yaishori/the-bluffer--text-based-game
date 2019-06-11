package assignment3.server;

public interface Game {
	public void start(ProtocolCallback<TBGPMessage> callback);
	public String sendTxtResp(String clientAddress, String answer, String originalCommand);
	public String sendSelectResp(String clientAddress, String answer, String originalCommand);
}
