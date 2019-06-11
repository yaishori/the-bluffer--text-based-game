package assignment3.server.reactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.util.Map;

import assignment3.server.ProtocolCallback;
import assignment3.server.TBGPMessage;
import assignment3.server.tokenizer.FixedSeparatorMessageTokenizer;
import assignment3.server.tokenizer.MessageTokenizer;

public class AsyncBroadcastProtocolCallback<T> implements ProtocolCallback<T> {
	private Map<String, AsyncConnectionHandler<T>> clientConnectionHandlers;
	private final FixedSeparatorMessageTokenizer tokenizer;
	
	public AsyncBroadcastProtocolCallback(Map<String, AsyncConnectionHandler<T>> clientConnectionHandlers,
			                              FixedSeparatorMessageTokenizer tokenizer) {
		this.clientConnectionHandlers = clientConnectionHandlers;
		this.tokenizer = tokenizer;
	}

	@Override
	public void sendMessage(T msg) throws IOException {
		TBGPMessage message = (TBGPMessage) msg;
		
		if (message.getRoomClientAddresses() != null) {
			AsyncConnectionHandler<T> connectionHandler;
			
			for (String clientAddress : message.getRoomClientAddresses()) {
				connectionHandler = clientConnectionHandlers.get(clientAddress);
				if (connectionHandler != null) {
					try {
		               ByteBuffer bytes = tokenizer.getBytesForMessage(message);
		               connectionHandler.addOutData(bytes);
		            }
		            catch (CharacterCodingException e) {
		            	e.printStackTrace();
		            }
				}
			}
		}
	}

}
