package assignment3.server;

import java.util.HashSet;
import java.util.Set;

public class Room {
	private String roomName;
	private Set<String> clientAddresses;
	private Game game;
	private boolean gameStarted;
	
	public Room(String name) {
		this.roomName = name;
		this.clientAddresses = new HashSet<String>();
		this.game = null;
		this.gameStarted = false;
	}
	
	public String getRoomName() {
		return roomName;
	}

	public Set<String> getClientAddresses() {
		return clientAddresses;
	}

	public Game getGame() {
		return game;
	}

	public boolean isGameStarted() {
		return gameStarted;
	}

	public void setGame(Game game) {
		this.game = game;
	}

	public void setGameStarted(boolean gameStarted) {
		this.gameStarted = gameStarted;
	}
}
