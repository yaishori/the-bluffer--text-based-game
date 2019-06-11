package assignment3.server;

public class SysMessage {
	private String orignalCommand;
	private ResultEnum resultEnum;
	private String customMessage;
	
	public SysMessage(String orignalCommand, ResultEnum resultEnum, String customMessage) {
		this.orignalCommand = orignalCommand;
		this.resultEnum = resultEnum;
		this.customMessage = customMessage;
	}
	
	public String getMessage() {
		return "SYSMSG " + orignalCommand + " " + resultEnum.toString() + (customMessage != null ? " " + customMessage : "");
	}
}
