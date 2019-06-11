package assignment3.server;

import java.net.InetAddress;
import java.util.Set;

public class TBGPMessage {
	private String clientAddress;
	private String message;
	private Set<String> roomClientAddresses;
	
	public TBGPMessage(String clientAddress, String message, Set<String> roomClientAddresses) {
		this.clientAddress = clientAddress;
		this.message = message;
		this.roomClientAddresses = roomClientAddresses;
	}

	public String getClientAddress() {
		return clientAddress;
	}

	public String getMessage() {
		return message;
	}

	public Set<String> getRoomClientAddresses() {
		return roomClientAddresses;
	}
}
