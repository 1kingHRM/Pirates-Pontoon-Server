package dream;


import game.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static game.Constants.*;

/**
 * This class is used to represent the server.
 * It takes care of connecting to individual clients and as well as attending to their requests
 */
public class Server 
{
	private static volatile Server server; // The server itself. Its volatile so all other threads will not have copies which would cause issues.
	// It is also static so it can be accessed from other classes

	private final String address; // The IP Address on which the server is running
	private final int port; // The port on which the server is listening

	private final Game game; // The game running on the server
	private final Client[] clients; // The list of players

	private final int maxPlayers; // The maximum players on this server
	private int playerCount = 0; // The total number of players;

	/**
	 * Starts up the server and runs the game
	 * @param address the IP address of the server
	 * @param port the port of the server
	 * @param maxPlayers the maximum number of players that will be accepted
	 * @param rounds the number of rounds to be played in one match
	 */
	public static void startServer(String address, int port, int maxPlayers, int rounds) throws Exception
	{
		if(maxPlayers < 1 || maxPlayers > 4) // If the players are less than 1 or greater than 4
			throw new Exception("Minimum Players: 1 Maximum Players: 4"); // Throw an exception

		if(rounds < 1) // If the rounds are less than 1
			throw new Exception("Minimum Rounds: 1"); // Throw an exception

		Server.server = new Server(address, port, maxPlayers, rounds); // Create a new server
		Server.server.start(); // Start the server
	}

	/**
	 * Create a server
	 * @param address the IP address of the server
	 * @param port the port of the server
	 * @param maxPlayers the maximum number of players
	 * @param rounds the number of rounds to be played in a single game
	 */
	private Server(String address, int port, int maxPlayers, int rounds)
	{
		this.address = address;
		this.port = port;
		this.game = new Game(rounds); // Create a new game
		this.maxPlayers = maxPlayers;
		this.clients = new Client[maxPlayers]; // Create an array of connections
	}

	/**
	 * Get the maximum number of players on the server
	 * @return the maximum players the server can support
	 */
    public static int max()
    {
    	return Server.server.maxPlayers;
    }

	/**
	 * Check if the server can start the game
	 * @return true if all the players are ready or false if otherwise
	 */
	public static boolean shouldStart()
	{
		boolean response = true;
		for(Client client : Server.server.clients) // For each flag
			response = response && client.isReady(); // AND the flag with the response.
		return response; // response will only be true if all clients are ready but false if otherwise
	}

	/**
	 * Readies all the players and starts the game
	 */
    private void start()
	{
		getPlayers(); // Get all the player connections
		startGame(); // Start the game
	}

	/**
	 * Creates all the player connections
	 */
	private void getPlayers()
	{
		ServerSocket serverSocket = null; // Declare a new serverSocket

		try
		{
			serverSocket = new ServerSocket(this.port, 0, InetAddress.getByName(this.address)); // Create the serverSocket and listen on the specified URL and port number.
			System.out.println("Server is up and running!");
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			System.exit(1); // Close the server and exit the program if the serverSocket was not created.
		}

		while(this.playerCount < this.maxPlayers) // While the server is up and running.
		{
			Socket client; // Declare a client socket.
			try
			{
				client = serverSocket.accept(); // Assign the client to the incoming socket received by the serverSocket
				this.clients[this.playerCount] = new Client(client); // create a new connection
				System.out.println("Connected to Client " + client.getInetAddress());

				new Thread(this.clients[this.playerCount]).start(); // Wrap each connection in a thread and start it
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			this.playerCount++; // increment the number of players
		}
	}

	/**
	 * Starts a game of Pirates Pontoon
	 */
	private void startGame()
	{
		while(!shouldStart()) // While all the players are not ready
		{
			System.out.println("Waiting for the clients before starting game");
			sleep(1000); // Sleep for 1 second
		}

		this.game.set(this.clients); // Send the players to the game

		sleep(1000); // Sleep for 1 second

		boolean firstWelcome = false;

		while (this.game.hasMoreRounds()) // While there are still more rounds to be played
		{
			playRound(firstWelcome); // Play a single round
			this.game.reset(); // Reset the game
			sleep(2000); // Sleep for 2 seconds

			firstWelcome = true;
		}

		broadcast(GAME_OVER); // Inform all clients that the game is over
		broadcast(QUIT); // Inform all clients to quit
	}

	/**
	 * Play a single round of Pirates Pontoon
	 */
	private void playRound(boolean welcome)
	{
		broadcast(START_ROUND); // Inform all the clients that the round is about to start
		sleep(1000); // Sleep for 1 second

		if(!welcome)
			broadcast(MESSAGE + " Welcome to Pirates Pontoon"); // Welcome Message

		this.game.playRound(); // Play one round

		sleep(1000); // Sleep for 1 second

		sendHighScores(); // Inform all the clients about the high scores
	}

	/**
	 * Send the high scores of the game to all the clients
	 */
	private void sendHighScores()
	{
		String scores = getHighScores(); // Get the string form of the high scores

		broadcast(END_ROUND); // Inform all the clients that the round is over
		sleep(1000); // Sleep for 1 second
		broadcast(HIGH_SCORES + " " + scores); // Inform all clients about the high scores
	}

	/**
	 * Converts the high scores into a string which can be sent to all the players.
	 * @return the String representation of the high scores
	 */
	public static String getHighScores()
	{
		int[] roundScores = Server.server.game.getHighScores(); // Get the high scores of the game
		StringBuilder scores = new StringBuilder(); // Create a new String Builder
		for(int i = 0; i < Server.server.clients.length; ++i) // For each connection in the server
		{
			boolean isActive = Server.server.clients[i].isReady();
			if(!isActive)
				continue;

			String name = Server.server.clients[i].player().name(); // Get the name of the player
			scores.append(name)
					.append(" ").append(roundScores[i]); // append their name and their score
			if(i <= Server.server.clients.length - 1)
				scores.append(" ");
		}
		return scores.toString();
	}

	/**
	 * Suspend the execution for a specified milliseconds
	 * @param milliseconds the time in milliseconds in which the server will stop operation
	 */
	public static void sleep(int milliseconds)
	{
		try
		{
			Thread.sleep(milliseconds);
		}
		catch (InterruptedException ignored)
		{

		}
	}

	/**
	 * Send a message to all the players
	 * @param message the message to be sent
	 */
	public static void broadcast(String message)
	{
		for(Client client : Server.server.clients) // for each connection
		{
			if(!client.isReady())
				continue;

			client.send(BROADCAST + " " + message); // send the message to the connection
			System.out.println(BROADCAST + " " + message);
		}
	}

	/**
	 * Ask the player if they would like to deal or hold
	 * @param client the connection being asked
	 * @return the response of the player, whether Deal or Hold
	 */
	public static String askAndBroadcast(Client client)
	{
		boolean firstBroadcast = true;
		String playerName = client.player().name(); // Get the name of the player in the connection
		while(true) // Continually execute this request loop
		{
			client.send(ASK); // Ask the connection
			if(firstBroadcast)
			{
				broadcast(ASK + " " + playerName + " was asked by the dealer whether to Deal or Hold");
				firstBroadcast = false;
			}

			if(client.isDealing()) // if the connection chose to Deal
			{
				client.clear(); // clear its flag
				broadcast(ASK + " " + playerName + " chose to Deal");
				return DEAL;
			}
			else if(client.isHolding()) // if the connection chose to Hold
			{
				client.clear(); // clear its flag
				broadcast(ASK + " " + playerName + " chose to Hold");
				return HOLD;
			}
			else
				sleep(500); // There is no response from the client, sleep for half a second then try again
		}
	}

	/**
	 * Get the connections
	 * @return the array of all the players
	 */
	public static synchronized Client[] clients()
	{
		return Server.server.clients;
	}

	/**
	 * Check if the server is still up and running
	 * @return true if the game still has more rounds to play ir false if otherwise
	 */
	public static synchronized boolean active()
	{
		return Server.server.game.hasMoreRounds();
	}

	/**
	 * Get the number of players connected to the server
	 * @return the total number of players connected
	 */
	public static synchronized int status()
	{
		return Server.server.playerCount;
	}

}