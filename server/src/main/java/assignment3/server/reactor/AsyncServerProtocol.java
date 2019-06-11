package assignment3.server.reactor;

import assignment3.server.ProtocolCallback;
import assignment3.server.ServerProtocol;

interface AsyncServerProtocol<T> extends ServerProtocol<T> {
	
	String processMessage(T msg, ProtocolCallback<T> callback);
	
	boolean isEnd(T msg);
	
	boolean shouldClose();
	
	void connectionTerminated();
}