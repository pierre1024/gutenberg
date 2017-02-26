package gutenberg.server;

import gutenberg.AConfig;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "server")
public class CConfig extends AConfig
{

	private String rssFilename;
	private String catalogFilename;

	private String rssURL;
	private String catalogURL;
	
	@XmlElement
	public String getRssURL()
	{
		return rssURL;
	}
	public void setRssURL(String rssURL)
	{
		this.rssURL = rssURL;
	}
	@XmlElement
	public String getCatalogURL()
	{
		return catalogURL;
	}
	public void setCatalogURL(String catalogURL)
	{
		this.catalogURL = catalogURL;
	}


	@XmlElement
	public String getRssFilename()
	{
		return rssFilename;
	}
	public void setRssFilename(String rssFilename)
	{
		this.rssFilename = rssFilename;
	}
	@XmlElement
	public String getCatalogFilename()
	{
		return catalogFilename;
	}
	public void setCatalogFilename(String catalogFilename)
	{
		this.catalogFilename = catalogFilename;
	}
	
	// Build config class from xml
	public static CConfig build(InputStream configStream)
	{
		try
		{
			JAXBContext jaxbContext = JAXBContext.newInstance(CConfig.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			return (CConfig)jaxbUnmarshaller.unmarshal(configStream);

		} catch (JAXBException e)
		{
			e.printStackTrace();
		}
		return null;// TBI
	}

}
