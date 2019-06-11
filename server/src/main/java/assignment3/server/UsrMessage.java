package assignment3.server;

public class UsrMessage {
	private String userName;
	private String message;
	
	public UsrMessage(String userName, String message) {
		this.userName = userName;
		this.message = message;
	}
	
	public String getMessage() {
		return "USRMSG " + userName + ": " + message;
	}
}
