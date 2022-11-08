package game;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to represent a deck of cards
 */
public class Deck
{
    private final List<Card> cards = new ArrayList<>(); // A list for all the cards this deck contains

    /**
     * Create a new deck of cards
     */
    public Deck()
    {
        createCards(); // create the cards and add them to the list
    }

    /**
     * Clear all the cards in the deck and create new ones
     */
    public void reset()
    {
        this.cards.clear();
        createCards();
    }

    /**
     * Create a new set of cards
     */
    private void createCards()
    {
        for(int i = 0; i < Constants.numberOfSuits; i++) // For each suit
        {
            for(int j = 0; j < Constants.numberOfValues; ++j) // For each value
            {
                int value = (j >= 10) ? 10 : (j + 1); // If the value is a court card, then it has a value of 10 otherwise, it has a normal value
                this.cards.add(new Card(Constants.suits[i], Constants.values[j], value)); // create a new card and add it to the list of cards
            }
        }
    }

    /**
     * Remove a card from the deck
     * @param index the index of the card to be removed
     * @return the card removed from the deck
     */
    public Card removeCard(int index)
    {
        return this.cards.remove(index);
    }

    /**
     * Get the size of the deck
     * @return the number of cards left in the deck
     */
    public int size()
    {
        return this.cards.size();
    }
}
