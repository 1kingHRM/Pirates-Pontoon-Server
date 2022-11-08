package dream;

import game.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static game.Constants.*;

/**
 * This class represents a player connection to the server and is used to interact with the server.
 */
public class Client implements Runnable
{
    private Player player; // The player it encapsulates
    private final Socket socket; // The socket connecting the client to the server
    private final PrintWriter writer; //For sending server responses to the client
    private final BufferedReader reader; // For receiving client responses

    private boolean deal; // A flag to indicate if the player chose to deal another card
    private boolean hold; // A flag to indicate if the player chose to hold their card
    private boolean ready; // A flag to indicate if the player is ready for the game to begin

    /**
     * Create a new Connection object to connect the server with a client via a socket
     * @param socket the socket connecting the server and the client
     * @throws Exception if the streams of the socket were not obtained
     */
    public Client(Socket socket) throws Exception
    {
        this.socket = socket;
        this.writer = new PrintWriter(socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.deal = false;
        this.hold = false;
        this.ready = false;

        String[] playerDetails = this.receive().split(" "); // Receive the name of the player from the client
        if(playerDetails[0].equals(PLAYER_NAME))
            this.player = new Player(playerDetails[1]);
    }

    @Override
    public void run()
    {
        while(Server.active())
        {
            String message = receive(); // Receive messages from the client
            if(message.equals(QUIT)) // If it is a quit message, the client quit unexpectedly
            {
                this.ready = false;

                Server.broadcast(MESSAGE + " " + this.player.name() + " has left the game."); // Inform the others

                break; // Break this connection
            }

            String response = parse(message); // Parse the message and get the response
            send(response); // Send the response back to the client
        }

        try
        {
            this.socket.close(); // Close the socket
        }
        catch (Exception ignored)
        {

        }
    }

    /**
     * Indicates if the user wants to deal another card
     * @return true if the response was Deal and false otherwise
     */
    public boolean isDealing()
    {
        return this.deal;
    }

    /**
     * Indicates if the user wants to hold their current card
     * @return true if the response was Hold and false otherwise
     */
    public boolean isHolding()
    {
        return this.hold;
    }

    /**
     * Indicates if the user is ready
     * @return true if the user is ready for the game or false if otherwise
     */
    public boolean isReady()
    {
        return this.ready;
    }

    /**
     * Resets the dealing and holding flags
     */
    public void clear()
    {
        this.deal = false;
        this.hold = false;
    }

    /**
     * Parse the messages received from the client
     * @param message the data sent by the client
     * @return the response of the server
     */
    private String parse(String message)
    {
        StringBuilder response = new StringBuilder(); // Create an empty string builder
        if(message.startsWith(ALL_NAMES)) // if the client wants the names of all the players in the server
        {
            Client[] players = Server.clients(); // get the connections from the server
            for(int i = 0; i < players.length; ++i) // For each connection in the server
            {
                response.append(players[i].player().name()); // get the name and append it
                if(i <= players.length - 1)
                    response.append(" ");
            }
        }
        else if(message.equals(READY)) // if the client is ready
            this.ready = true; // Set the ready flag
        else if(message.equals(CONNECTION_STATUS)) // if the client wants the number of available players
            response.append(Server.status()); // append the number of players connected
        else if(message.equals(MAX_PLAYERS)) // if the player wants the maximum number of players
            response.append(Server.max()); // append the maximum players
        else if(message.startsWith(DEAL)) // if the client responded with Deal
            this.deal = true; // Set the dealing flag to true
        else if(message.startsWith(HOLD)) // if the client responded with Hold
            this.hold = true; // set the holding flag to true
        return response.toString(); // return the response
    }

    /**
     * Send a message to the client
     * @param message the message to be sent
     */
    public void send(String message)
    {
        this.writer.println(message);
        this.writer.flush();
    }

    /**
     * Receive a message from the client
     * @return the message received from the client
     */
    public String receive()
    {
        try
        {
            return this.reader.readLine();
        }
        catch(IOException e)
        {
            return "";
        }
    }

    /**
     * Get the player this connection is encapsulating
     * @return the player object contained in this connection
     */
    public Player player()
    {
        return this.player;
    }
}
