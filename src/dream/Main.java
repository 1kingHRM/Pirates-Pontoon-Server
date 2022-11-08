package dream;

public class Main
{

    public static void main(String[] args) throws Exception
    {
        if(args.length != 4)
            throw new Exception("Proper Arguments: IP_Address Port Maximum_Players Rounds");

        String address = args[0];
        int port, maxPlayers, rounds;

        try
        {
            port = Integer.parseInt(args[1]);
            maxPlayers = Integer.parseInt(args[2]);
            rounds = Integer.parseInt(args[3]);
        }
        catch (NumberFormatException ex)
        {
            throw new Exception("Illegal Integer Values For Port, Maximum_Players or Rounds");
        }

	    Server.startServer(address, port, maxPlayers, rounds); // Start the server
    }
}
