package assignment3.server;

public interface ServerProtocol<T> {
	
	String processMessage(T msg, ProtocolCallback<T> callback);
	
	boolean isEnd(T msg);
	
}