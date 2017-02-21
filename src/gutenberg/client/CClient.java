package gutenberg.client;

import gutenberg.AConfig;
import gutenberg.CActor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.lang.String;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
* Client is an actor who realize work package
* 
* @author	Pierre Cathary
* @version	1.0
* @see CActor
*/
public class CClient extends CActor
{
	private CConfig config;
	private static final String CLIENT_CONFIG_FILENAME="rc/client.xml";
	public CClient()
	{
		this(CLIENT_CONFIG_FILENAME);
	}
	public CClient(String configFilename)
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

	/**
	 * publish work package
	 */
	void publishWP(JSONObject request) throws Exception
	{
		getLogger().log(Level.INFO,"running publish request "+request.toString(3)+" ...");
		
		//Build process
		ProcessBuilder pb = new ProcessBuilder("curl","http://localhost:5279/lbryapi", "--data",request.toString());
		Process process;
		
		//Run process with timeout
		process = pb.start();
		if(!process.waitFor(config.getProcessTimeout(), TimeUnit.SECONDS))
		{ 
			process.destroyForcibly();
			String msg="process timeout";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		//Get error if any
		BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

		String line;
		String sErr="";
		while ((line = err.readLine()) != null)
		{
			sErr+=line;
		}
		//we have an error abort
		if(!sErr.isEmpty())
		{
			String msg="publish request report error {"+sErr+"}";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
	
		BufferedReader answer = new BufferedReader(new InputStreamReader(process.getInputStream()));
		
		//Get answer if any
		String sAnswer="";
		while ((line = answer.readLine()) != null)
		{
			sAnswer+=line;
		}
		if(sAnswer.isEmpty())
		{
			String msg="publish answer is empty";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		//answer must be a JSON
		JSONObject janswer;
		try
		{
			janswer=new JSONObject(sAnswer);
		}
		catch(JSONException exception)
		{
			String msg="publish answer is an invalid JSON : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);	
		}
		if(janswer.has("error"))
		{
			String msg="publish answer has an error : "+janswer.get("error").toString();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		
		getLogger().log(Level.INFO,"publish request done!");
		
		//Request is done we wait for inter process delay (seconds)
		Thread.sleep(config.getInterProcessDelay()*1000);
	}
	

	/**
	 * commit work package status
	 */
	void commitWPStatus(int id,boolean succeed) throws Exception
	{
		String sId=Integer.toString(id);
		String sStatus=succeed?"DONE_2":"ABORT";
		
		getLogger().log(Level.INFO,"commit work package {"+sId+"} status {"+sStatus+"} ...");
		
		
		Statement statementUpdate = connection.createStatement();
		String sql = "UPDATE WORK_PACKAGE set STATUS = '"+sStatus+"' where ID="+sId+";";
		statementUpdate.executeUpdate(sql);
		
		getLogger().log(Level.INFO,"commit done!");
	}
	
	/**
	 * 
	 * @param jsonParam contains all data about book
	 * @param id book id
	 * @throws Exception 
	 */
	void processWP(JSONObject jsonParam,int id) throws Exception
	{

		getLogger().log(Level.INFO,"processing working package {"+Integer.toString(id)+"} ...");
		
		/************************************************
		 * Create publish request
		 ************************************************/
		JSONObject jsonRequest=new JSONObject();
		jsonRequest.put("jsonrpc","2.0");
		jsonRequest.put("method","publish");
		JSONArray vParam=new JSONArray();
		jsonRequest.put("params",vParam);
		jsonRequest.put("id",(int)id++);
		vParam.put(jsonParam);

		//TBI make alt path
		String path=jsonParam.getString("file_path");
		String alt="http://aleph.gutenberg.org/cache/epub/<ID>/pg<ID>-images.epub";
		path=alt.replaceAll("<ID>",Integer.toString(id));
		
		//name must contain only alphadecimal characters and '-' so we have to transform the string
		String name=jsonParam.getString("name");

		//first remove all diacritics
		name=Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

		//now we replace all other characters by '-'
		name=name.replaceAll("[^0-9a-zA-Z]+","-");

		//name can't start or end with '-'
		name=name.replaceAll("^[-]+","").replaceAll("[-]+$","");

		jsonParam.put("name",name);


		JSONObject jsonMeta=jsonParam.getJSONObject("metadata");
		if(jsonMeta.has("creator"))
		{
			jsonMeta.put("author",jsonMeta.get("creator"));
			jsonMeta.remove("creator");
		}
		if(!jsonMeta.has("author"))
		{
			jsonMeta.put("author","Unknown");
		}
		
		jsonParam.put("bid",config.getBid());
		jsonParam.remove("ver");
		jsonMeta.put("nsfw", jsonMeta.get("nsfw").equals("true"));

		/************************************************
		 * Download ebook file to cache folder
		 ************************************************/
		String cacheFilename=config.getCacheFolder()+File.separator+name+".epub";

		getLogger().log(Level.INFO,"downloading file {"+path+"} to {"+cacheFilename+"} ...");

		try
		{
			FileUtils.copyURLToFile(new URL(jsonParam.getString("file_path")), new File(cacheFilename));
		}
		catch (IOException exception)
		{
			String msg="download failed with exception : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		getLogger().log(Level.INFO,"download done!");
		
		jsonParam.put("file_path",cacheFilename);
	}
	
	/**
	 * Publish all work packages
	 * */
	public void run() throws Exception
	{
		

		/************************************************
		 * 		Initialize DB and log systems
		 ************************************************/
		init();
		
		/************************************************
		 * 		Extract all work packages from database
		 ************************************************/
		getLogger().log(Level.INFO,"extracting all work packages from database {WORK_PACKAGE} ...");
		
		ResultSet sqlResult;
		try
		{

			Statement statement = connection.createStatement();
			String sql = "SELECT ID, METADATA FROM WORK_PACKAGE WHERE STATUS = 'AVAILABLE';";
			sqlResult = statement.executeQuery(sql);
		}
		catch(SQLException exception)
		{
			String msg="extracting all work packages failed : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		
		getLogger().log(Level.INFO,"extract done, fetch all work packages {"+sqlResult.getFetchSize()+"}!");

		/************************************************
		 * 		Process all work packages from database
		 ************************************************/
		getLogger().log(Level.INFO,"processing all work packages {"+sqlResult.getFetchSize()+"}!");
		
		while (sqlResult.next())
		{
			int wpId=sqlResult.getInt("ID");
			try
			{
				processWP(new JSONObject(sqlResult.getString("METADATA")),wpId);
			}
			catch(Exception exception)
			{
				commitWPStatus(wpId,false);
			}
			commitWPStatus(wpId,true);
		}
		getLogger().log(Level.INFO,"processing all work packages done!");
	}

	@Override
	public AConfig getConfig()
	{
		return config;
	}
}
