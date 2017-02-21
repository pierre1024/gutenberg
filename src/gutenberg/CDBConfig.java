package gutenberg;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;


/**
* DBConfig hold all configurations relative to database
* 
* @author	Pierre Cathary
* @version	1.0
*/
@XmlRootElement(name = "DBConfig")
public class CDBConfig
{

	private String addr;
	private int port;
	private String name;
	private String user;
	private String password;
	private String driver;
	private String jdbcName;


	@XmlElement
	public String getJdbcName()
	{
		return jdbcName;
	}

	public void setJdbcName(String jdbcName)
	{
		this.jdbcName = jdbcName;
	}

	@XmlElement
	public void setDriver(String driver)
	{
		this.driver = driver;
	}
	
	public String getDriver()
	{
		return driver;
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

	@XmlElement
	public void setAddr(String addr)
	{
		this.addr = addr;
	}

	public String getAddr()
	{
		return addr;
	}

	@XmlElement
	public void setPort(int port)
	{
		this.port = port;
	}
	
	public int getPort()
	{
		return port;
	}


	@XmlElement
	public void setUser(String user)
	{
		this.user = user;
	}
	
	public String getUser()
	{
		return user;
	}


	@XmlElement
	public void setPassword(String password)
	{
		this.password = password;
	}
	
	public String getPassword()
	{
		return password;
	}

	public String getPath()
	{
		String request="jdbc:"+getJdbcName()+"://";
		request+=getAddr()+":"+getPort()+"/";
		request+=getName();
		return request;
	}


}
