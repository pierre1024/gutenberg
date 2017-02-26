package gutenberg.client;

import gutenberg.AConfig;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
* Config holds all configurations relative to a client
* 
* @author	Pierre Cathary
* @version	1.0
* @see CClient
*/
@XmlRootElement(name = "client")
public class CConfig extends AConfig
{

	private String cacheFolder;
	private Double bid;
	private int processTimeout;
	private int interProcessDelay;

	@XmlElement
	public int getInterProcessDelay()
	{
		return interProcessDelay;
	}
	public void setInterProcessDelay(int interProcessDelay)
	{
		this.interProcessDelay = interProcessDelay;
	}
	@XmlElement
	public int getProcessTimeout()
	{
		return processTimeout;
	}
	public void setProcessTimeout(int processTimeout)
	{
		this.processTimeout = processTimeout;
	}
	@XmlElement
	public Double getBid()
	{
		return bid;
	}
	public void setBid(Double bid)
	{
		this.bid = bid;
	}
	@XmlElement
	public String getCacheFolder()
	{
		return cacheFolder;
	}
	public void setCacheFolder(String cacheFolder)
	{
		this.cacheFolder = cacheFolder;
	}
	
	// Build config class from xml
	public static CConfig build(InputStream configStream) throws Exception
	{
		try
		{
			
			JAXBContext jaxbContext = JAXBContext.newInstance(CConfig.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			return (CConfig)jaxbUnmarshaller.unmarshal(configStream);
		} catch (JAXBException exception)
		{
			throw new Exception("Invalid config stream, parser exception : "+exception.getMessage());
		}
	}

}
