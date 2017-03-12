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
			int nbPackage=1;
			if(args.length>0)
			{
				nbPackage=Integer.parseInt(args[0]);
			}
			client.run(nbPackage);
			System.exit(0);
		}
		//something goes wrong we abort program
		catch (Exception exception)
		{
			System.out.println("client.CMain> unexpected exception {"+exception.getMessage()+"}");
			System.exit(-1);
		}
	}

}
