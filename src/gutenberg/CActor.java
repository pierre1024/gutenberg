package gutenberg;


import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* Actor class defines a user that play a role with the database
* 
* @author	Pierre Cathary
* @version	1.0
*/

public abstract class CActor
{
	protected Connection connection;

	private Logger logger;
	protected String configFilename;
	public abstract AConfig getConfig();
	public CActor(String configFilename)
	{
		this.configFilename=configFilename;
	}
	/**
	 * @return logger
	 * */
	public Logger getLogger()
	{
		return logger;
	}
	
	/**
	 * initialize database and log systems
	 * @throws Exception 
	 */
	public void init() throws Exception
	{
		
		/************************************************
		 * initialize log system
		 ************************************************/
		
		//first we get log configuration from xml
		CLogConfig logConf=getConfig().getLog();

		//we create a logger and set the handler
		logger = Logger.getLogger(logConf.getName());
		try
		{
			FileHandler logHandler=new FileHandler(logConf.getFilename(), logConf.getMaxFileSize(),logConf.getMaxFileNumber());
			logger.addHandler(logHandler);
			getLogger().log(Level.INFO,"log system {"+logConf.getName()+"} initialized!");
		}
		catch (SecurityException | IOException exception)
		{
			//something happens on initialization we throw exception
			String msg="LOG system {"+logConf.getName()+"} initialization failed {"+exception.getMessage()+"}!";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		

		
		/************************************************
		 * initialize database system
		 ************************************************/

		//first we get db configuration from xml
		CDBConfig dbConf=getConfig().getDB();
		
		getLogger().log(Level.INFO,"connecting to DB {"+dbConf.getName()+"} ...");
		
		try
		{
			Class.forName(dbConf.getDriver());
			connection = DriverManager.getConnection(dbConf.getPath(),dbConf.getUser(),dbConf.getPassword());
			getLogger().log(Level.INFO,"connection established!");
		}
		catch (SQLException exception)
		{
			//bad passeword/username invalid syntax,... we abort connection
			String msg="DB {"+dbConf.getName()+"} connection failed {"+exception.getMessage()+"}!";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		catch (ClassNotFoundException exception)
		{
			//db driver missing... we abort connection
			String msg="unable to find DB driver {"+dbConf.getDriver()+"}!";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
	}
		
}
