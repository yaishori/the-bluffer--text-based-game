package assignment3.server;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

class MultipleClientProtocolServer implements Runnable {
	private ServerSocket serverSocket;
	private int listenPort;
	private ServerProtocolFactory<TBGPMessage> factory;
	private Map<String, ConnectionHandler> clientConnectionHandlers;
	
	public MultipleClientProtocolServer(int port, ServerProtocolFactory<TBGPMessage> p)
	{
		serverSocket = null;
		listenPort = port;
		factory = p;
		clientConnectionHandlers = new HashMap<String, ConnectionHandler>();
	}
	
	public void run()
	{
		try {
			serverSocket = new ServerSocket(listenPort);
			System.out.println("Listening on port " + listenPort + "...");
		}
		catch (IOException e) {
			System.out.println("Cannot listen on port " + listenPort);
		}
		
		TBGPData tbgpData = new TBGPData();
		BroadcastProtocolCallback broadcastProtocolCallback = new BroadcastProtocolCallback(clientConnectionHandlers);
		
		while (true)
		{
			try {
				Socket acceptedSocket = serverSocket.accept();
				
				ConnectionHandler newConnection = new ConnectionHandler(acceptedSocket, factory.create(tbgpData), broadcastProtocolCallback);
				new Thread(newConnection).start();
				
				String clientAddress = acceptedSocket.getInetAddress() + ":" + acceptedSocket.getPort();
				clientConnectionHandlers.put(clientAddress, newConnection);
			}
			catch (IOException e)
			{
				System.out.println("Failed to accept on port " + listenPort);
			}
		}
	}

	// Closes the connection
	public void close() throws IOException
	{
		serverSocket.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		if (args.length != 2) {
            System.err.println("Usage: java MultipleClientProtocolServer <port> <jsonFileName>");
            System.exit(1);
        }
		
		// Get port
		int port = Integer.decode(args[0]).intValue();
		String jsonFileName = args[1];
		
		MultipleClientProtocolServer server = new MultipleClientProtocolServer(port, new TBGPFactory(jsonFileName));
		Thread serverThread = new Thread(server);
        serverThread.start();
		try {
			serverThread.join();
		}
		catch (InterruptedException e)
		{
			System.out.println("Server stopped");
		}
		
		
				
	}
}
