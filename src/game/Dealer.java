package game;

import java.util.Random;

/**
 * This class is used to represent a dealer
 */
public class Dealer
{
    private int score; // The score of the dealer

    public Dealer()
    {
        this.score = 0;
    }

    /**
     * Get the score of a dealer
     * @return the score of this dealer
     */
    public int score()
    {
        return this.score;
    }

    /**
     * Deals a player a card
     * @param deck the deck of cards from which the player is being dealt
     * @param player the player to be dealt a card
     * @return the card being dealt to the player
     */
    public Card dealPlayer(Deck deck, Player player)
    {
        Random r = new Random(System.currentTimeMillis()); // Create a new random generator

        int index = Math.abs(r.nextInt() % deck.size()); // Get a random index within the size of the remaining cards in the deck
        Card card = deck.removeCard(index); // Remove the card at the specified index
        player.add(card.score()); // add the score of the card to the player

        return card;
    }

    public void dealSelf(Deck deck)
    {
        Random r = new Random(System.currentTimeMillis()); // Create a new random generator

        while(this.score <= 16) // While the dealer has a score less than or equal to 16
        {
            int index = Math.abs(r.nextInt() % deck.size()); // Get a random index within the size of the remaining cards in the deck
            this.score += deck.removeCard(index).score(); // Remove the card at the specified index and add its score to the dealer's
        }
    }

    /**
     * Reset the score of the dealer to 0
     */
    public void reset()
    {
        this.score = 0;
    }

}
