package gutenberg.server;

import gutenberg.AConfig;
import gutenberg.CActor;
import gutenberg.server.CItemRegister;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.logging.Level;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
* Server is an actor who create work package from gutenberg web site
* 
* @author	Pierre Cathary
* @version	1.0
* @see CActor
*/
public class CServer extends CActor
{
	private CConfig config;
	private static final String SERVER_CONFIG_FILENAME="rc/server.xml";
	private static final String GUT_CACHE_FILE_TAG_OPEN="cache/epub/";
	private static final String GUT_CACHE_FILE_TAG_CLOSE="\">";
	public CServer()
	{
		this(SERVER_CONFIG_FILENAME);	
	}
	public CServer(String configFilename)
	{
		super(configFilename);
	}

	/**
	 * Initialize DB and log systems
	 */
	public void init() throws Exception
	{
		config=CConfig.build(configFilename);
		super.init();
	}
	

	public void fillDBFromCatalog() throws Exception
	{
		/************************************************
		 * 		Download catalog file
		 ************************************************/
		getLogger().log(Level.INFO,"downloading catalog file {"+config.getCatalogURL()+"} to {"+config.getCatalogFilename()+"} ...");

		try
		{
			FileUtils.copyURLToFile(new URL(config.getCatalogURL()), new File(config.getCatalogFilename()));
		}
		catch (IOException exception)
		{
			String msg="download catalog failed with exception : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		getLogger().log(Level.INFO,"download catalog done!");
		
		/************************************************
		 * 		Extract all items from catalog
		 ************************************************/
		getLogger().log(Level.INFO,"Extract all items from catalog ...");

		HashSet<String> sFileSet=new HashSet<String> ();
		File descriptor=new File("catalog.rdf");
		//extract all files paths
			/************************************************
			 * 		Extract all paths
			 ************************************************/
		getLogger().log(Level.INFO,"Extracting all paths ...");
		BufferedReader buffer=null;
		try
		{
			buffer=new BufferedReader(new FileReader(descriptor));
			for (String line=buffer.readLine(); line != null; line=buffer.readLine())
			{
			  if(line.indexOf(GUT_CACHE_FILE_TAG_OPEN)!=-1)
			  {
				  sFileSet.add(line.substring(line.lastIndexOf("/")+1).replaceAll(GUT_CACHE_FILE_TAG_CLOSE,"").trim());
			  }
			}
		}
		catch (IOException exception)
		{
			String msg="Extract paths failed on catalog : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		finally
		{
			try
			{
				if(buffer!=null){buffer.close();}
			}
			catch (IOException exception)
			{
				String msg="Extract paths failed on catalog, unable to close buffer : "+exception.getMessage();
				getLogger().log(Level.SEVERE,msg);
				throw new Exception(msg);
			}
		}
		getLogger().log(Level.INFO,"Extract all paths done!");
		
		/************************************************
		 * 		Extract all work packages
		 ************************************************/
		getLogger().log(Level.INFO,"Extracting all work packages ...");

		Element root=null;
		Document xml=null;
		try
		{
			DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			DocumentBuilder builder=factory.newDocumentBuilder();
			xml=builder.parse(descriptor);
		}
		catch (ParserConfigurationException | SAXException | IOException exception)
		{
			String msg="Extracting all work packages failed XML parse error : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		root=xml.getDocumentElement();
		//XPath requests are to slow here ...
		NodeList list=root.getChildNodes();
		int nbChild=list.getLength();
		int idBook=0;
		String msg;
		for(int idItem=0; idItem < nbChild; ++idItem)
		{
			if (list.item(idItem) instanceof Element)
			{
				Element element=(Element) list.item(idItem);
				if(element.getNodeName().equals("pgterms:etext"))
				{
					++idBook;
					String id=element.getAttribute("rdf:ID").replaceAll("[^0-9]", "");
					msg="Extracting progress : "+Integer.toString(idBook)+", idbook : "+id;
					getLogger().log(Level.INFO,msg);
					try
					{
						msg="register book : "+id+"...";
						getLogger().log(Level.INFO,msg);
						CItemRegister.run(connection,id,element,sFileSet);
						msg="register book : "+id+" done!";
					}
					catch (Exception exception)
					{
						msg="register book : "+id+" failed : "+exception.getMessage();
					}
				}
			}
		}
		getLogger().log(Level.INFO,"Extracting all work packages from catalog done!");
		
	}
	public void run() throws Exception
	{

		/************************************************
		 * 		Initialize DB and log systems
		 ************************************************/
		init();
		
		/************************************************
		 * 		Create table work package is not exists
		 ************************************************/
		getLogger().log(Level.INFO,"checking table {WORK_PACKAGE} ...");
		
		try
		{
			Statement statement=connection.createStatement();
			String sql =	"CREATE TABLE IF NOT EXISTS WORK_PACKAGE "	+
			"(ID 		INT PRIMARY KEY     NOT NULL,"		+
			" STATUS	CHAR(32)			NOT NULL,"		+
			" METADATA	JSON				NOT NULL)";
			
			statement.executeUpdate(sql);
		}
		catch(SQLException exception)
		{
			String msg="checking table {WORK_PACKAGE} failed : "+exception.getMessage();
			getLogger().log(Level.INFO,msg);
			throw new Exception(msg);
		}
		getLogger().log(Level.INFO,"checking table {WORK_PACKAGE} done!");

		
		
		/************************************************
		 * 		extract all items from catalog
		 ************************************************/
		File f=new File(config.getCatalogFilename());
		if(!f.exists())
		{
			fillDBFromCatalog();
		}

		/*TBI
		 * first we download catalog from gutenberg site : http://www.gutenberg.org/cache/epub/feeds/rdf-files.tar.zip
		 * we unzip file
		*/
		
		
		
		

	}
	@Override
	public AConfig getConfig()
	{
		return config;
	}
}
