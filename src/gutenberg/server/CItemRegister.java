package gutenberg.server;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;



public class CItemRegister
{

	//TBI : put all of this in xml...
    protected static final String GUT_ARRAY_TOKEN_TAG="rdf:Bag";

    protected static final String LBRY_METADATA_TOKEN="metadata";
    protected static final String LBRY_DESCRIPTION_TOKEN="description";
    protected static final String LBRY_CONTENT_TYPE_TOKEN="content_type";
    protected static final String GUT_CONTENT_TYPE="application/epub";
    protected static final String LBRY_PATH_TOKEN="file_path";
    protected static final String LBRY_NAME_TOKEN="name";
    protected static final String LBRY_VERSION_TOKEN="ver";
    protected static final String LBRY_VERSION_VALUE="1.0.0";
    protected static final String LBRY_ARRAY_TOKEN_TAG=", ";
    protected static final String LBRY_SUBJECT_TOKEN_TAG="subject";
    protected static final String LBRY_NSFW_TOKEN_TAG="nsfw";
    protected static final String LBRY_LICENCE_TOKEN_TAG="license";
    protected static final String LBRY_TITLE_TOKEN_TAG="title";
    protected static final String LBRY_THUMBNAIL_TOKEN_TAG="thumbnail";
    protected static final String LBRY_LICENCE_PUBLIC_VALUE="Public Domain";
    protected static final String GUT_CACHE_BASE_URL=" http://gutenberg.pglaf.org/cache/generated/<ID>/";
    protected static final String GUT_CACHE_TEMPLATE_THUMBNAIL="pg<ID>.cover.medium.jpg";
    protected static final String GUT_CACHE_TEMPLATE_EPUB="pg<ID>-images.epub";
    protected static final String GUT_CACHE_TEMPLATE_ALT_EPUB="pg<ID>.epub";
    
	protected static final Map<String,String> mMetaKey = new HashMap<String, String>();
    static {
    	mMetaKey.put("title", LBRY_TITLE_TOKEN_TAG);
    	mMetaKey.put("title", LBRY_DESCRIPTION_TOKEN);
    	mMetaKey.put("friendlytitle",LBRY_TITLE_TOKEN_TAG);
    	mMetaKey.put("creator", "creator");
    	mMetaKey.put("subject", "subject");
    	mMetaKey.put("language", "language");
    }
	protected static final ArrayList<String> vNSFW = new ArrayList<String>();
    static {
    	vNSFW.add("sex");
    	vNSFW.add("fight");
    }
	
	enum EState
	{
		ABORT,
		DONE,
		AVAILABLE
	}
	
	public static String getRNodeValue(Node node)
	{
		if(node==null){return "";}
		String value="";
		if(node.hasChildNodes())
		{
			int itemId=0;
			boolean isGroup=false;
			for(;itemId<node.getChildNodes().getLength();++itemId)
			{
				if(node.getChildNodes().item(itemId).getNodeName().equals(GUT_ARRAY_TOKEN_TAG))
				{
					isGroup=true;
					break;
				}
			}
			if(isGroup)
			{
				NodeList fieldList=node.getChildNodes().item(itemId).getChildNodes();
				int nbField=fieldList.getLength();
				String nextField;
				for(int idField = 0; idField < nbField; ++idField)
				{
					nextField=getRNodeValue(fieldList.item(idField));
					if(!nextField.isEmpty())
					{
						value+=nextField;
						value+=LBRY_ARRAY_TOKEN_TAG;
					}
				}
				return value.replaceAll(LBRY_ARRAY_TOKEN_TAG+"$", "");
			}
			else
			{
				return getRNodeValue(node.getChildNodes().item(0));
			}
		}
		else
		{
			if(node.getNodeValue()==null){return "";}
			return node.getNodeValue().trim().replaceAll("^[ \t\n,;]+", "");
		}
	}
	static int HTTP_SUCCESS=200;
	static boolean checkURL(String sURL)
	{
		try
		{
			URL url;
			HttpURLConnection huc;
			url = new URL ( sURL);
			huc = ( HttpURLConnection )  url.openConnection ();
			huc.setRequestMethod ("GET");
			huc.connect () ;
			return huc.getResponseCode() == HTTP_SUCCESS;
		}
		catch (MalformedURLException e) {
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		return false;
	}
	static void run(Connection connection,String id,Element parent,HashSet<String> sFileSet) throws Exception
	{
		if(parent.getNodeName().indexOf("pgterms:etext")==-1 || !parent.hasChildNodes()){throw new Exception("item has no childs");}
		

		/************************************************
		 * 		at the beginning everything is all right
		 ************************************************/
		EState state=EState.AVAILABLE;
		String baseRCUrl=GUT_CACHE_BASE_URL.replaceAll("<ID>", id);
		/************************************************
		 * 		we build metadatas
		 ************************************************/
		JSONObject metaData=new JSONObject();
		NodeList metaList=parent.getChildNodes();
		int nbMeta=metaList.getLength();
		String metaName;
		for(int idMeta = 0; idMeta < nbMeta; ++idMeta)
		{
			Node node=metaList.item(idMeta);
			metaName=metaList.item(idMeta).getNodeName().replaceAll("^.*:", "");
			if(mMetaKey.containsKey(metaName))
			{
				metaData.put(mMetaKey.get(metaName),getRNodeValue(node));
			}
		}
		
			/************************************************
			 * 		we check if books is nswf
			 ************************************************/

		Boolean nsfw=false;
		if(metaData.has(LBRY_SUBJECT_TOKEN_TAG))
		{
			for(String itemNSFW : vNSFW)
			{
				if(metaData.get(LBRY_SUBJECT_TOKEN_TAG).toString().toLowerCase().indexOf(itemNSFW)!=-1)
				{
					nsfw=true;
					break;
				}
			}
		}
		if(!nsfw && metaData.has(LBRY_TITLE_TOKEN_TAG))
		{
			for(String itemNSFW : vNSFW)
			{
				if(metaData.has(LBRY_TITLE_TOKEN_TAG) && metaData.get(LBRY_TITLE_TOKEN_TAG).toString().toLowerCase().indexOf(itemNSFW)!=-1)
				{
					nsfw=true;
					break;
				}
			}
		}
		metaData.remove(LBRY_SUBJECT_TOKEN_TAG);
		metaData.put(LBRY_NSFW_TOKEN_TAG,nsfw);
		

			/************************************************
			 * 		all books's licences are public here
			 ************************************************/
		metaData.put(LBRY_LICENCE_TOKEN_TAG,LBRY_LICENCE_PUBLIC_VALUE);

	
			/************************************************
			 * 		if book has thumbnail we register it
			 ************************************************/

		String thumbPath=GUT_CACHE_TEMPLATE_THUMBNAIL.replaceAll("<ID>",id);
	    if(sFileSet.contains(thumbPath))
	    {
			metaData.put(LBRY_THUMBNAIL_TOKEN_TAG,baseRCUrl+thumbPath);
	    }


			/************************************************
			 * 		set content type
			 ************************************************/	

		metaData.put(LBRY_CONTENT_TYPE_TOKEN,GUT_CONTENT_TYPE);

		/************************************************
		 * 		we build request
		 ************************************************/
		JSONObject request=new JSONObject();
		request.put(LBRY_METADATA_TOKEN, metaData);
		

	    
		/************************************************
		 * 		get ebook url
		 ************************************************/

		String bookPath=GUT_CACHE_TEMPLATE_EPUB.replaceAll("<ID>",id);
	    if(!sFileSet.contains(bookPath))
	    {
			bookPath=GUT_CACHE_TEMPLATE_ALT_EPUB.replaceAll("<ID>",id);
	    	if(!sFileSet.contains(bookPath))
	    	{
	    		state=EState.ABORT;
	    	}
	    }
		request.put(LBRY_PATH_TOKEN, baseRCUrl+bookPath);

		/************************************************
		 * 		set version
		 ************************************************/
		//request.put(LBRY_VERSION_TOKEN, LBRY_VERSION_VALUE);
		
		/************************************************
		 * 		set ebook name
		 ************************************************/
		request.put(LBRY_NAME_TOKEN, metaData.get(LBRY_TITLE_TOKEN_TAG).toString().replaceAll("[\t ,]+", "-"));
		
		
		/************************************************
		 * 		we push book on database
		 ************************************************/
		try
		{
			Statement statement = connection.createStatement();
			String sql = "INSERT INTO WORK_PACKAGE (ID,STATUS,METADATA) ";
			sql+="VALUES ('"+id+"',";
			sql+="'"+state.toString()+"',";
			sql+="'"+request.toString().replaceAll("'", "''")+"') ON CONFLICT (ID) DO NOTHING;";
			//sql+="'"+request.toString().replaceAll("'", "''")+"') ON CONFLICT (ID) DO UPDATE SET ";
			//sql+="ID=EXCLUDED.ID, STATUS=EXCLUDED.STATUS, METADATA=EXCLUDED.METADATA;";
			statement.executeUpdate(sql);
			//"INSERT ... ON CONFLICT DO NOTHING/UPDATE"
		}
		catch (SQLException exception)
		{
			//TBI
			exception.printStackTrace();
		}
	}
}
