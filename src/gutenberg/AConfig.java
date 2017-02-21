package gutenberg;

import javax.xml.bind.annotation.XmlElement;

/**
* AConfig hold all configurations relative to actor
* 
* @author	Pierre Cathary
* @version	1.0
* @see CActor
*/
public abstract class AConfig
{
	private int first;
	private int last;
	private CDBConfig db;
	private CLogConfig log;
	

	@XmlElement(name="db",type=CDBConfig.class)
	public void setDB(CDBConfig db)
	{
		this.db = db;
	}
	public CDBConfig getDB()
	{
		return db;
	}

	@XmlElement(name="log",type=CLogConfig.class)
	public void setLog(CLogConfig log)
	{
		this.log = log;
	}
	public CLogConfig getLog()
	{
		return log;
	}


	@XmlElement
	public void setFirst(int first)
	{
		this.first = first;
	}

	public int getFirst()
	{
		return first;
	}

	@XmlElement
	public void setLast(int last)
	{
		this.last = last;
	}

	public int getLast()
	{
		return last;
	}
}
