package gutenberg;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
* LogConfig hold all configurations relative to log system
* 
* @author	Pierre Cathary
* @version	1.0
*/
@XmlRootElement(name = "LogConfig")
public class CLogConfig
{
	private String name;
	private String filename;
	private int maxFileSize;
	private int maxFileNumber;

	@XmlElement
	public int getMaxFileSize()
	{
		return maxFileSize;
	}

	public void setMaxFileSize(int maxFileSize)
	{
		this.maxFileSize = maxFileSize;
	}

	@XmlElement
	public int getMaxFileNumber()
	{
		return maxFileNumber;
	}

	public void setMaxFileNumber(int maxFileNumber)
	{
		this.maxFileNumber = maxFileNumber;
	}



	@XmlElement
	public String getFilename()
	{
		return filename;
	}

	public void setFilename(String filename)
	{
		this.filename = filename;
	}

	@XmlElement
	public void setName(String name)
	{
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
}