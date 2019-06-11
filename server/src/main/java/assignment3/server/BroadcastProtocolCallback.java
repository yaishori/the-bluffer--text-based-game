package assignment3.server;

import java.io.IOException;
import java.util.Map;

public class BroadcastProtocolCallback implements ProtocolCallback<TBGPMessage> {
	private Map<String, ConnectionHandler> clientConnectionHandlers;
	
	public BroadcastProtocolCallback(Map<String, ConnectionHandler> clientConnectionHandlers) {
		this.clientConnectionHandlers = clientConnectionHandlers;
	}

	@Override
	public void sendMessage(TBGPMessage msg) throws IOException {
		if (msg.getRoomClientAddresses() != null) {
			ConnectionHandler connectionHandler;
			
			for (String clientAddress : msg.getRoomClientAddresses()) {
				connectionHandler = clientConnectionHandlers.get(clientAddress);
				if (connectionHandler != null) {
					connectionHandler.sendMessage(msg.getMessage());
				}
			}
		}
	}

}
