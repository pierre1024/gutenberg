package gutenberg.server;

import gutenberg.server.CServer;

public class CMain
{

	/**
	 * @param args
	 * @throws JAXBException 
	 */
	public static void main(String[] args)
	{
		//Build server and run it
		try
		{
			CServer server=new CServer();
			server.run();
			System.exit(0);
		}
		//something goes wrong we abort program
		catch (Exception exception)
		{
			System.exit(-1);
		}
	}

}
