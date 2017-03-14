package gutenberg.client;

import gutenberg.AConfig;
import gutenberg.CActor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.logging.Level;
import java.lang.String;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
	private static final String CLIENT_CONFIG_FILENAME="client.xml";
	protected enum EWPCol
	{
		ID,
		STATUS,
		METADATA
	}
	/**
	 * publish work package
	 */
	protected void publishWP(JSONObject request) throws Exception
	{
		getLogger().log(Level.INFO,"running publish request "+request.toString(3)+" ...");

		/*
		//Build process
		ProcessBuilder pb = new ProcessBuilder("cursl","http://localhost:5279/lbryapi", "--data",request.toString());
		Process process;
		
		//Run process with timeout
		process = pb.start();
		if(!process.waitFor(config.getProcessTimeout(), TimeUnit.SECONDS))
		{ 
			process.destroyForcibly();
			String msg="process timeout";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}*/
		HttpClient httpClient = HttpClientBuilder.create().build();

	    HttpPost httpRequest = new HttpPost("http://localhost:5279/lbryapi");
	    String sAnswer="";
	    try
		{
		    httpRequest.setEntity(new StringEntity(request.toString()));
		    httpRequest.setHeader("Accept", "application/json");
		    httpRequest.setHeader("Content-type", "application/json");
		    HttpResponse response = httpClient.execute(httpRequest);
		    sAnswer=EntityUtils.toString(response.getEntity());
		    System.out.print(sAnswer);
		} catch (IOException e)
		{
			String msg="publish request failed";
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		} 

		//we have an error abort
		if(sAnswer.isEmpty())
		{
			String msg="publish answer is empty, abort publishing...";
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
	protected void commitWPStatus(int id,boolean succeed) throws Exception
	{
		String sId=Integer.toString(id);
		String sStatus=succeed?"DONE_2":"ABORT";
		
		getLogger().log(Level.INFO,"commit work package {"+sId+"} status {"+sStatus+"} ...");
		
		
		//Statement statementUpdate = connection.createStatement();
		//String sql = "UPDATE WORK_PACKAGE set STATUS = '"+sStatus+"' where ID="+sId+";";
		//statementUpdate.executeUpdate(sql);
		
		getLogger().log(Level.INFO,"commit done!");
	}

	
	protected void makePackage(int idPackage) throws Exception
	{
		getLogger().log(Level.INFO,"extracting work package {"+Integer.toString(idPackage)+"} from database {WORK_PACKAGE} ...");
		ResultSet sqlResult=null;
		try
		{

			Statement statement = connection.createStatement();
			String sql = "SELECT * FROM public.fetch_backup();";
			sqlResult = statement.executeQuery(sql);
			
		}
		catch(SQLException exception)
		{
			String msg="extracting work package {"+Integer.toString(idPackage)+"} failed : "+exception.getMessage();
			getLogger().log(Level.SEVERE,msg);
			throw new Exception(msg);
		}
		
		getLogger().log(Level.INFO,"extract done, fetch all work packages {"+sqlResult.getFetchSize()+"}!");

		/************************************************
		 * 		Process all work packages from database
		 ************************************************/
		int nbItem=100;
		getLogger().log(Level.INFO,"processing all work packages {"+nbItem+"}!");
		
		
		int idItem=1;
		while (sqlResult.next())
		{
			int wpId=sqlResult.getInt("ID");
			String msg="\n####### PACKAGE {"+idPackage+"} ITEM {"+idItem+"/"+nbItem+"} ... #######\n";
			++idItem;
			getLogger().log(Level.INFO,msg);
			try
			{
				processWP(new JSONObject(sqlResult.getString("METADATA")),wpId);
			}
			catch(Exception exception)
			{
				commitWPStatus(wpId,false);
				msg="\n####### PACKAGE {"+idPackage+"} ITEM {"+idItem+"/"+nbItem+"} ABORT! #######\n";
				getLogger().log(Level.INFO,msg);
				continue;
			}
			commitWPStatus(wpId,true);
			msg="\n####### PACKAGE {"+idPackage+"} ITEM {"+idItem+"/"+nbItem+"} DONE! #######\n";
			getLogger().log(Level.INFO,msg);
		}
		getLogger().log(Level.INFO,"processing all work packages done!");
	}
	/**
	 * 
	 * @param jsonParam contains all data about book
	 * @param id book id
	 * @throws Exception 
	 */
	protected void processWP(JSONObject jsonParam,int id) throws Exception
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

		//name can't start or end with '-' and all lower case
		name=name.replaceAll("^[-]+","").replaceAll("[-]+$","").toLowerCase();

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
		String cacheFolder=config.getCacheFolder();
		if(cacheFolder.isEmpty())
		{
			cacheFolder=System.getProperty("user.dir")+File.separator+"cache";
		}
		String cacheFilename=cacheFolder+File.separator+name+".epub";

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
		publishWP(jsonRequest);
	}
	
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
		InputStream configStream = getClass().getClassLoader().getResourceAsStream(configFilename); 
		if(configStream==null)
		{
			throw new Exception("CClient::init> invalid configuration, unable to find config filename {"+configFilename+"}");
		}
		try
		{
			config=CConfig.build(configStream);
			super.init();	
		}
		finally
		{
			configStream.close();
		}
	}

	
	
	/**
	 * Publish all work packages
	 * @param nbPackage 
	 * */
	public void run(int nbPackage) throws Exception
	{
		

		/************************************************
		 * 		Initialize DB and log systems
		 ************************************************/
		init();
		
		/************************************************
		 * 		Extract all work packages from database
		 ************************************************/
		getLogger().log(Level.INFO,"extracting all work packages {"+Integer.toString(nbPackage)+"} from database {WORK_PACKAGE} ...");
		
		for(int idPackage=0;idPackage<nbPackage;++idPackage)
		{
			makePackage(idPackage);
		}
	}

	@Override
	public AConfig getConfig()
	{
		return config;
	}
}
