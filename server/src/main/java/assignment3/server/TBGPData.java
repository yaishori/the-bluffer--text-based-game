package assignment3.server;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TBGPData {
	private Map<String, String> nicknames;
	private Map<String, Room> rooms;
	private Set<String> supportedGameNames;
	
	public TBGPData() {
		nicknames = new HashMap<String, String>();
		rooms = new HashMap<String, Room>();
		supportedGameNames = new HashSet<String>();
		supportedGameNames.add(BlufferGame.NAME);
	}

	public Map<String, String> getNicknames() {
		return nicknames;
	}

	public Map<String, Room> getRooms() {
		return rooms;
	}

	public Set<String> getSupportedGameNames() {
		return supportedGameNames;
	}
}
