package gutenberg.client;

/**
 * This application takes ebooks metadata from a data base and publish it on the lbry network 
* 
* @author	Pierre Cathary
* @version	1.0
*/
public class CMain
{

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args)
	{
		//Build client and run it
		try
		{
			CClient client=new CClient();
			client.run();
			System.exit(0);
		}
		//something goes wrong we abort program
		catch (Exception exception)
		{
			System.exit(-1);
		}
	}

}
