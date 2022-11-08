package game;

import dream.Client;
import dream.Server;

import static game.Constants.*;
import static game.Constants.MESSAGE;

/**
 * This class is used to represent a game of Pirates Pontoon. It takes care of creating the dealer, the deck of cards
 * and as well as playing a number of rounds that is specified during creation time.
 */
public class Game
{
    private final Deck deck; // A deck of cards
    private final Dealer dealer; // The dealer of cards

    private Player[] players; // An array of players
    private int[] highScores; // The high scores for the total game

    private final int maxRounds; // The maximum rounds that can be played
    private int currentRound; // The current round being played

    /**
     * Creates a new game object.
     * @param maxRounds the maximum rounds to be played
     */
    public Game(int maxRounds)
    {
        this.deck = new Deck(); // Create a new deck of cards
        this.dealer = new Dealer(); // Create a new dealer
        this.maxRounds = maxRounds;
        this.currentRound = 0;
    }

    /**
     * Set the players for the game
     * @param clients the players playing the game
     */
    public void set(Client[] clients)
    {
        this.players = new Player[clients.length];
        for(int i = 0; i < clients.length; i++)
            this.players[i] = clients[i].player();

        this.highScores = new int[players.length];
    }

    /**
     * Check if more rounds can still be played
     * @return true if more rounds can still be played or false if otherwise
     */
    public boolean hasMoreRounds()
    {
        return this.currentRound < this.maxRounds;
    }

    /**
     * Reset the contents of the deck and the score of the dealer.
     */
    public void reset()
    {
        this.dealer.reset();
        this.deck.reset();
    }

    /**
     * Returns the high scores
     * @return the high scores
     */
    public int[] getHighScores()
    {
        return this.highScores;
    }

    /**
     * Play a single round of Pirates Pontoon
     */
    public void playRound()
    {
        dealAllPlayers(); // Deal each player two random cards
        askEachPlayer(); // Ask each player if they want to hold or deal another card

        dealer.dealSelf(deck); // Deal the dealer until its score is more than 16

        Server.sleep(1000); // Sleep the server for 1 second

        determineWinner(); // Determine the winner of the round

        ++this.currentRound; // Increment the current round variable
    }

    /**
     * Deals two random cards to every player in the game.
     */
    private void dealAllPlayers()
    {
        Client[] clients = Server.clients();

        for(Client client : clients) // For each player
        {
            if(!client.isReady())
                continue;

            Player player = client.player();

            Card card = this.dealer.dealPlayer(this.deck, player); // deal the player a card from the deck

            String currentPlayer = player.name();
            String cardDetails = card.value() + " " + card.suit() + " " + card.score(); // get the details of the card dealt

            Server.broadcast(DEAL_CARD + " " + currentPlayer + " " + cardDetails); // send the details to all clients

            Server.sleep(1000); // Sleep the server for 1 second

            String suitOne = card.suit(), valueOne = card.value();

            card = this.dealer.dealPlayer(this.deck, player); // deal the player another card from the deck
            cardDetails = card.value() + " " + card.suit() + " " + card.score(); // get its details

            Server.broadcast(DEAL_CARD + " " + currentPlayer + " " + cardDetails); // send the details to all clients

            String suitTwo = card.suit(), valueTwo = card.value();

            Server.broadcast(END); // signals the end of transmission
            Server.broadcast(MESSAGE + " " + currentPlayer
                    + " was dealt two cards: A " + valueOne + " of " + suitOne +
                    " and a " + valueTwo + " of " + suitTwo); // send a message to all clients about the cards the current player was dealt

            Server.sleep(500); // sleep the server for half a second
        }
    }

    /**
     * Ask each player if they want to deal another card or hold their current card
     */
    private void askEachPlayer()
    {
        Client[] clients = Server.clients(); // Get the connections from the server since we are going to be sending a request to the connection

        for (Client client : clients) // For each connection
        {
            if(!client.isReady())
                continue;

            String playerName = client.player().name(); // Get the name of the player represented by this connection

            String response = Server.askAndBroadcast(client); // Ask the player if they would like to hold or deal another card

            while (response.equals(DEAL)) // while the player wants to deal more cards
            {
                Card card = dealer.dealPlayer(deck, client.player()); // Deal the card
                String cardDetails = card.value() + " " + card.suit()
                        + " " + card.score(); // get its details
                Server.broadcast(DEAL_CARD + " " + playerName + " " + cardDetails); // send it to all players
                Server.broadcast(END); // end the transmission
                Server.broadcast(MESSAGE + " " + playerName + " was dealt a " + card.value() + " of " + card.suit()); // Send a message to every player's log

                if(client.player().score() > 21) // if the player's score was over 21, then the player is busted
                {
                    Server.broadcast(MESSAGE + " " + playerName + " was busted!");
                    break;
                }

                response = Server.askAndBroadcast(client); // Ask the player again if they want to deal another card or hold their current card
            }

            Server.sleep(1000); // Sleep the server for 1 second
        }
    }

    /**
     * Determines the winner after a single round of Pirates Pontoon
     */
    private void determineWinner()
    {
        int dealerScore = this.dealer.score(); // Get the score of the dealer

        Client[] clients = Server.clients();

        Server.broadcast(DEALER_SCORE + " " + dealerScore); // Update the clients about the score of the dealer
        Server.broadcast(MESSAGE + " The dealer has been dealt his cards"); // Send a message to each client's log

        Server.sleep(1000); // Sleep the server for 1 second

        if(dealerScore > 21) // The dealer has lost, everyone wins;
        {
            for(int i = 0; i < this.players.length; ++i)
                this.highScores[i] += 1; // Increment everybody's score
            Server.broadcast(MESSAGE + " The dealer had a score of " + dealerScore + " and lost this round. Everyone wins");
            Server.broadcast(WIN + " " + ALL);
        }
        else // The dealer did not lose
        {
            // Get the maximum score
            int maxScore = -1, index = -1;
            for(int i = 0; i < this.players.length; ++i)
            {
                int playerScore = this.players[i].score();
                if(playerScore > dealerScore && playerScore <= 21 && playerScore >= maxScore)
                {
                    maxScore = playerScore;
                    index = i;
                }
            }

            if(dealerScore >= maxScore) // If the dealer's score is at least the maximum score, the dealer wins
            {
                Server.broadcast(MESSAGE + " " + "The dealer wins this round");
                Server.broadcast(WIN + " -1");
            }
            else // Otherwise, a player won the round
            {
                // Determine how many people have the same score as the maximum score
                int count = 0;
                for (Player player : this.players)
                {
                    int playerScore = player.score();
                    if (playerScore == maxScore)
                        ++count;
                }
                if(count > 1) // If there are more than one person, it is a draw
                    Server.broadcast(MESSAGE + " " + "Draw! Nobody wins this round");
                else // Only one person wins the round
                {
                    this.highScores[index] += 1;
                    Server.broadcast(MESSAGE + " " + this.players[index].name() + " wins this round");
                    Server.broadcast(WIN + " " + index);
                }
            }
        }

        // Reset the scores of all the players
        for(Client client : clients)
        {
            if(!client.isReady())
                continue;

            client.player().reset();
        }
    }
}
