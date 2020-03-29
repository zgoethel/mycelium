package net.jibini.cliff.network.session;

import java.lang.reflect.Constructor;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jibini.cliff.routing.Request;

public class Session
{
	private Logger log = LoggerFactory.getLogger(getClass());
	
	private UUID sessionUUID;
	private String token;
	private SessionKernel kernel = null;
	
	private Session()
	{}
	
	public static Session create(SessionPlugin plugin, Request creationRequest)
	{
		Session result = new Session();
		result.sessionUUID = UUID.fromString(creationRequest.getHeader().getString("session"));
		result.generateToken();
		creationRequest.getResponse().put("token", result.token);
		
		try
		{
			Constructor<?> construct = plugin.getKernelClass().getConstructor(Session.class);
			result.kernel = (SessionKernel)construct.newInstance(result);
		} catch (Throwable t)
		{
			result.log.error("Failed to create session kernel", t);
		}
		
		return result;
	}
	
	public static Session create(UUID uuid, String token)
	{
		Session result = new Session();
		result.sessionUUID = uuid;
		result.token = token;
		return result;
	}
	
	public static Session create(String uuid, String token)
	{
		return create(UUID.fromString(uuid), token);
	}
	
	public void embed(Request request)
	{
		request.getHeader().put("session", sessionUUID.toString());
		request.getHeader().put("token", token);
	}
	
	private void generateToken()
	{
		int leftLimit = 48;
		int rightLimit = 122; 
		int targetStringLength = 32;
		
		Random random = new Random();
		for (int i = 0; i < 10; i ++)
			random.nextLong();
		random.setSeed(random.nextLong());

		String generatedString = random.ints(leftLimit, rightLimit + 1)
				.filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
		this.token = generatedString;
	}
	
	public UUID getSessionUUID()  { return sessionUUID; }
	
	public String getToken()  { return token; }
	
	public SessionKernel getKernel()  { return kernel; }
}