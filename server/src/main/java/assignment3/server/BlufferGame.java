package assignment3.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import assignment3.server.json.Question;
import assignment3.server.json.Questions;

public class BlufferGame implements Game {
	public static final String NAME = "BLUFFER";
	private String jsonFileName;
	private Room gameRoom;
	private TBGPData tbgpData;
	private List<Question> questions;
	private int currentQuestionIndex;
	private Map<String, Set<String>> answerClientAddresses;
	private Set<String> waitingForAnswerClientAddresses;
	private ProtocolCallback<TBGPMessage> callback;
	private boolean waitingForTxtResp;
	private boolean waitingForSelectResp;
	private List<String> answers;
	private Map<String, Integer> clientAddressScores;
	private Map<String, Integer> clientAddressRoundScores;
	private Map<String, String> clientAddressRoundAnswers;

	public BlufferGame(String jsonFileName, Room gameRoom, TBGPData tbgpData) {
		this.jsonFileName = jsonFileName;
		this.gameRoom = gameRoom;
		this.tbgpData = tbgpData;
		this.questions = loadRandomQuetions();
		this.currentQuestionIndex = 0;
		this.answerClientAddresses = new HashMap<String, Set<String>>();
		initWaitingForAnswerClientAddresses();
		this.callback = null;
		this.waitingForTxtResp = true;
		this.waitingForSelectResp = false;
		this.answers = null;
		initClientAddressScores();
	}
	
	private void initWaitingForAnswerClientAddresses() {
		this.waitingForAnswerClientAddresses = new HashSet<String>();
		for (String clientAddress : gameRoom.getClientAddresses()) {
			this.waitingForAnswerClientAddresses.add(clientAddress);
		}
	}
	
	private void initClientAddressScores() {
		this.clientAddressScores = new HashMap<String, Integer>();
		for (String clientAddress : gameRoom.getClientAddresses()) {
			this.clientAddressScores.put(clientAddress, 0);
		}
	}
	
	private void initClientAddressRoundScoresAndAnswers() {
		this.clientAddressRoundScores = new HashMap<String, Integer>();
		this.clientAddressRoundAnswers = new HashMap<String, String>();
		for (String clientAddress : gameRoom.getClientAddresses()) {
			this.clientAddressRoundScores.put(clientAddress, 0);
			this.clientAddressRoundAnswers.put(clientAddress, null);
		}
	}
	
	private List<Question> loadRandomQuetions() {
		String jsonString = readJsonFile();
		Gson gson = new Gson();
		Questions questionsJson = gson.fromJson(jsonString, Questions.class);
		List<Question> allQuestions = questionsJson.getQuestions();
		
		List<Question> randomQuestions = new ArrayList<Question>();
		Random random = new Random();
		int questionIndex;
		
		for (int i = 0; i < 3; i++) {
			questionIndex = random.nextInt(allQuestions.size());
			randomQuestions.add(allQuestions.get(questionIndex));
			allQuestions.remove(questionIndex);
		}
		
		return randomQuestions;
	}
	private String readJsonFile() {
		StringBuffer result = new StringBuffer();
		String line = null;
		BufferedReader bufferedReader = null;

		try {
			bufferedReader = new BufferedReader(new FileReader(jsonFileName));
					

			while ((line = bufferedReader.readLine()) != null) {
				result.append(line.trim());
			}

			bufferedReader.close();
		}
		catch (FileNotFoundException exception) {
			System.out.println("Unable to open file '" + jsonFileName + "'");
		}
		catch (IOException exception) {
			System.out.println("Error reading file '" + jsonFileName + "'");
		}
		finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException exception) {
				}
			}
		}
		
		return result.toString();
	}

	@Override
	public void start(ProtocolCallback<TBGPMessage> callback) {
		this.callback = callback;
		
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
				}
				askTxt();
			}
		});
		
		thread.start();
	}
	
	private void askTxt() {
		initWaitingForAnswerClientAddresses();
		initClientAddressRoundScoresAndAnswers();
		Question question = questions.get(currentQuestionIndex);
		answerClientAddresses.clear();
		waitingForTxtResp = true;
		waitingForSelectResp = false;
		
		TBGPMessage askTxtMessage = new TBGPMessage(null,
										            "ASKTXT " + question.getQuestionText(),
										            gameRoom.getClientAddresses());
		try {
			callback.sendMessage(askTxtMessage);
		} catch (IOException ex) {
		}
	}

	@Override
	public String sendTxtResp(String clientAddress, String answer, String originalCommand) {
		if (!waitingForTxtResp) {
			return new SysMessage(originalCommand, ResultEnum.REJECTED, null).getMessage();
		}
		
		answer = answer.toLowerCase();
		
		synchronized(waitingForAnswerClientAddresses) {
			if (!waitingForAnswerClientAddresses.contains(clientAddress)) {
				return new SysMessage(originalCommand, ResultEnum.REJECTED, "You already supplied an answer.").getMessage();
			}
			synchronized(answerClientAddresses) {
				Set<String> clientAddresses = answerClientAddresses.get(answer);
				if (clientAddresses == null) {
					clientAddresses = new HashSet<String>();
					answerClientAddresses.put(answer, clientAddresses);
				}
				clientAddresses.add(clientAddress);
				waitingForAnswerClientAddresses.remove(clientAddress);
				if (waitingForAnswerClientAddresses.isEmpty()) {
					
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(100);
							} catch (InterruptedException ex) {
							}
							askChoices();
						}
					});
					
					thread.start();
				}
			}
		}
		
		return new SysMessage(originalCommand, ResultEnum.ACCEPTED, null).getMessage();
	}
	
	private void askChoices() {
		initWaitingForAnswerClientAddresses();
		Question question = questions.get(currentQuestionIndex);
		waitingForTxtResp = false;
		waitingForSelectResp = true;
		
		List<String> unshuffledAnswers = new ArrayList<String>();
		answers = new ArrayList<String>();
		
		unshuffledAnswers.add(question.getRealAnswer());
		for (String answer : answerClientAddresses.keySet()) {
			if (!unshuffledAnswers.contains(answer)) {
				unshuffledAnswers.add(answer);
			}
		}
		
		Random random = new Random();
		int answerIndex;
		
		while(!unshuffledAnswers.isEmpty()) {
			answerIndex = random.nextInt(unshuffledAnswers.size());
			answers.add(unshuffledAnswers.get(answerIndex));
			unshuffledAnswers.remove(answerIndex);
		}
		
		StringBuffer answersStringBuffer = new StringBuffer();
		for (int i = 0; i < answers.size(); i++) {
			answersStringBuffer.append(i + ". " + answers.get(i) + " ");
		}
		
		TBGPMessage askTxtMessage = new TBGPMessage(null,
										            "ASKCHOICES " + answersStringBuffer.toString(),
										            gameRoom.getClientAddresses());
		try {
			callback.sendMessage(askTxtMessage);
		} catch (IOException ex) {
		}
	}

	@Override
	public String sendSelectResp(String clientAddress, String answerIndexString, String originalCommand) {
		if (!waitingForSelectResp) {
			return new SysMessage(originalCommand, ResultEnum.REJECTED, null).getMessage();
		}
		
		Question question = questions.get(currentQuestionIndex);
		
		int answerIndex = Integer.parseInt(answerIndexString);
		if (answerIndex < 0 || answerIndex >= answers.size()) {
			return new SysMessage(originalCommand, ResultEnum.REJECTED, "Invalid answer number given.").getMessage();	
		}
		String answer = answers.get(answerIndex);
		synchronized(clientAddressRoundAnswers) {
			clientAddressRoundAnswers.put(clientAddress, answer);
		}
		
		synchronized(waitingForAnswerClientAddresses) {
			if (!waitingForAnswerClientAddresses.contains(clientAddress)) {
				return new SysMessage(originalCommand, ResultEnum.REJECTED, "You already chose an answer.").getMessage();	
			}
			synchronized(clientAddressScores) {
				if (answer.equals(question.getRealAnswer())) {
					addPoints(clientAddress, 10);
				}
				else if (answerClientAddresses.containsKey(answer)) {
					for (String currentClientAddress : answerClientAddresses.get(answer)) {
						addPoints(currentClientAddress, 5);
					}
				}
				waitingForAnswerClientAddresses.remove(clientAddress);
				if (waitingForAnswerClientAddresses.isEmpty()) {
					
					Thread thread = new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								Thread.sleep(100);
							} catch (InterruptedException ex) {
							}
							
							sendRoundSummary(question.getRealAnswer());
							
							currentQuestionIndex++;
							if (currentQuestionIndex < questions.size()) {
								askTxt();
							}
							else {
								sendScoreSummary();
							}
						}
					});
					
					thread.start();
				}
			}
		}
		
		return new SysMessage(originalCommand, ResultEnum.ACCEPTED, null).getMessage();
	}
	
	private void addPoints(String clientAddress, int points) {
		clientAddressScores.put(clientAddress, clientAddressScores.get(clientAddress) + points);
		clientAddressRoundScores.put(clientAddress, clientAddressRoundScores.get(clientAddress) + points);
	}
	
	private void sendRoundSummary(String correctAnswer) {
		TBGPMessage message = new TBGPMessage(null,
			            "GAMEMSG The correct answer is: " + correctAnswer,
			            gameRoom.getClientAddresses());
		try {
			callback.sendMessage(message);
		} catch (IOException ex) {
		}
		
		for (String clientAddress : gameRoom.getClientAddresses()) {
			Set<String> addresses = new HashSet<String>();
			addresses.add(clientAddress);
			
			String correctOrWrong = clientAddressRoundAnswers.get(clientAddress).equals(correctAnswer) ? "correct" : "wrong";
			
			message = new TBGPMessage(null,
		            "GAMEMSG " + correctOrWrong + "! +" + clientAddressRoundScores.get(clientAddress) + "pts",
		            addresses);
				
			try {
				callback.sendMessage(message);
			} catch (IOException ex) {
			}
		}
	}
	
	private void sendScoreSummary() {
		StringBuffer scoreSummary = new StringBuffer();
		int i = 0;
		for (String clientAddress : clientAddressScores.keySet()) {
			i++;
			scoreSummary.append(tbgpData.getNicknames().get(clientAddress) + ": " + clientAddressScores.get(clientAddress) + "pts");
			if (i < clientAddressScores.size()) {
				scoreSummary.append(", ");
			}
		}
		
		TBGPMessage message = new TBGPMessage(null,
			            "GAMEMSG Summary: " + scoreSummary.toString(),
			            gameRoom.getClientAddresses());
		try {
			callback.sendMessage(message);
		}
		catch (IOException ex) {
		}
		
		synchronized(gameRoom) {
			gameRoom.setGame(null);
			gameRoom.setGameStarted(false);
		}
	}
}
