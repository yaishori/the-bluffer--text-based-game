package assignment3.server;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TBGP implements ServerProtocol<TBGPMessage> {
	private TBGPData tbgpData;
	private String jsonFileName;
	protected boolean hasQuitted;
	protected boolean isConnectionTerminated;
	
	public TBGP(TBGPData tbgpData, String jsonFileName) {
		this.tbgpData = tbgpData;
		this.jsonFileName = jsonFileName;
		this.hasQuitted = false;
		this.isConnectionTerminated = false;
	}
	
	public String processMessage(TBGPMessage msg, ProtocolCallback<TBGPMessage> callback)
	{
		if (isConnectionTerminated) {
			return null;
		}
		
		String message = msg.getMessage();
		String[] commandParts = message.split(" ");
		String command = commandParts[0];
		String parameter = message.substring(message.indexOf(" ") + 1);
		String clientAddress = msg.getClientAddress();
		
		String currentNickname;
		synchronized(tbgpData.getNicknames()) {
			currentNickname = tbgpData.getNicknames().get(clientAddress);
		}
		
		if (!command.equals("NICK") &&
		    !command.equals("JOIN") &&
		    !command.equals("MSG") &&
		    !command.equals("LISTGAMES") &&
		    !command.equals("STARTGAME") &&
		    !isEnd(msg) &&
		    !command.equals("TXTRESP") &&
		    !command.equals("SELECTRESP")) {
			return new SysMessage(command, ResultEnum.REJECTED, "Invalid command.").getMessage();
		}
		
		if (command.equals("NICK")) {
			if (currentNickname != null) {
				return new SysMessage(command, ResultEnum.REJECTED, "You already have a nickname").getMessage();
			}
			else {
				synchronized(tbgpData.getNicknames()) {
					if (tbgpData.getNicknames().values().contains(parameter)) {
						return new SysMessage(command, ResultEnum.REJECTED, "Nickname " + parameter + " is already taken").getMessage();
					}
					else {
						tbgpData.getNicknames().put(clientAddress, parameter);
						return new SysMessage(command, ResultEnum.ACCEPTED, null).getMessage();
					}
				}
			}
		}
		else if (currentNickname == null) {
			return new SysMessage(message, ResultEnum.UNIDENTIFIED, "Please specify a nickname first.").getMessage();
		}
		else if (command.equals("JOIN")) {
			Room currentRoom = getCurrentRoom(clientAddress);
			
			synchronized(tbgpData.getRooms()) {
				Room desiredRoom = tbgpData.getRooms().get(parameter);
				
				if (desiredRoom != null && currentRoom != null && desiredRoom == currentRoom) {
					return new SysMessage(command, ResultEnum.REJECTED, "You already joined " + currentRoom.getRoomName()).getMessage();
				}
				
				if (currentRoom != null && currentRoom.isGameStarted()) {
					return new SysMessage(command, ResultEnum.REJECTED, "You can't leave " + currentRoom.getRoomName() + " while the game is on.").getMessage();
				}
				
				if (desiredRoom != null && desiredRoom.isGameStarted()) {
					return new SysMessage(command, ResultEnum.REJECTED, "You can't join " + desiredRoom.getRoomName() + " while the game is on.").getMessage();
				}
				
				if (currentRoom != null) {
					currentRoom.getClientAddresses().remove(clientAddress);
				}
				
				if (desiredRoom == null) {
					desiredRoom = new Room(parameter);
					tbgpData.getRooms().put(parameter, desiredRoom);
				}
				
				desiredRoom.getClientAddresses().add(clientAddress);
				
				return new SysMessage(command, ResultEnum.ACCEPTED, null).getMessage();
			}
		}
		else if (command.equals("MSG")) {
			String messageToSend = new UsrMessage(tbgpData.getNicknames().get(clientAddress), parameter).getMessage();
			Room currentRoom = getCurrentRoom(clientAddress);
			if (currentRoom == null) {
				return new SysMessage(command, ResultEnum.REJECTED, "you have to join a room first.").getMessage();
			}
			
			Set<String> clientAddresses = new HashSet<String>();
			clientAddresses.addAll(currentRoom.getClientAddresses());
			clientAddresses.remove(clientAddress);
			
			TBGPMessage broadcastMessage = new TBGPMessage(clientAddress,
					                                       messageToSend,
					                                       clientAddresses);
			try {
				callback.sendMessage(broadcastMessage);
				return new SysMessage(command, ResultEnum.ACCEPTED, null).getMessage();
			} catch (IOException e) {
				return new SysMessage(command, ResultEnum.REJECTED, "Error sending the message").getMessage();
			}
		}
		else if (command.equals("LISTGAMES")) {
			StringBuffer gameNames = new StringBuffer();
			gameNames.append("Game List: ");
			for (String gameName : tbgpData.getSupportedGameNames()) {
				gameNames.append(gameName + ", ");
			}
			
			return new SysMessage(command, ResultEnum.ACCEPTED, gameNames.substring(0, gameNames.length() - 2)).getMessage();
		}
		else if (command.equals("STARTGAME")) {
			Room currentRoom = getCurrentRoom(clientAddress);;
			Game game = null;
			String gameName = parameter;
			
			if (currentRoom == null) {
				return new SysMessage(command, ResultEnum.REJECTED, "Please join a room first.").getMessage(); 
			}
			
			synchronized(currentRoom) {
				if (currentRoom.getGame() != null) {
					return new SysMessage(command, ResultEnum.REJECTED, "A game is already started.").getMessage(); 
				}
				
				if (!tbgpData.getSupportedGameNames().contains(gameName)) {
					return new SysMessage(command, ResultEnum.REJECTED, "Game is not supported.").getMessage();
				}
				
				if (gameName.equals(BlufferGame.NAME)) {
					game = new BlufferGame(jsonFileName, currentRoom, tbgpData);
					currentRoom.setGame(game);
					currentRoom.setGameStarted(true);
				}
			}
			
			if (game != null) {
				game.start(callback);
			}
			
			return new SysMessage(command, ResultEnum.ACCEPTED, null).getMessage();
		}
		else if (isEnd(msg)) {
			hasQuitted = true;
			
			Room currentRoom = getCurrentRoom(clientAddress);
			if (currentRoom != null) {
				synchronized(currentRoom) {
					if (currentRoom.getGame() != null) {
						return new SysMessage(message, ResultEnum.REJECTED, "Cannot quit while the game is on.").getMessage();
					}
					currentRoom.getClientAddresses().remove(clientAddress);
				}
			}
			
			synchronized(tbgpData.getNicknames()) {
				tbgpData.getNicknames().remove(clientAddress);
			}
			
			return new SysMessage(command, ResultEnum.ACCEPTED, null).getMessage();
		}
		
		else if (getGame(clientAddress) == null) {
			return new SysMessage(message, ResultEnum.REJECTED, "No active game.").getMessage();
		}
		else if (command.equals("TXTRESP")) {
			Game game = getGame(clientAddress);
			if (game == null){
				return new SysMessage(command, ResultEnum.REJECTED, "No active game.").getMessage();	
			}
			return game.sendTxtResp(clientAddress, parameter, command);
		}
		else if (command.equals("SELECTRESP")) {
			Game game = getGame(clientAddress);
			if (game == null){
				return new SysMessage(command, ResultEnum.REJECTED, "No active game.").getMessage();	
			}
			return game.sendSelectResp(clientAddress, parameter, command);
		}
		else {
			return new SysMessage(command, ResultEnum.REJECTED, "Invalid command.").getMessage();	
		}
	}

	private Room getCurrentRoom(String clientAddress) {
		Room room;
		synchronized(tbgpData.getRooms()) {
			for (String roomName : tbgpData.getRooms().keySet()) {
				room = tbgpData.getRooms().get(roomName);
				if (room.getClientAddresses().contains(clientAddress)) {
					return room;
				}
			}
		}
		return null;
	}
	
	private Game getGame(String clientAddress) {
		Room room = getCurrentRoom(clientAddress);
		if (room == null){
			return null;
		}
		synchronized(room){
			return room.getGame();
		}
	}
	
	public boolean isEnd(TBGPMessage msg)
	{
		return msg.getMessage().equals("QUIT");
	}
}