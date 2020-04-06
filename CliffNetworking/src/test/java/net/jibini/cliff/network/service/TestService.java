package net.jibini.cliff.network.service;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.jibini.cliff.network.session.Session;
import net.jibini.cliff.network.session.SessionKernel;
import net.jibini.cliff.network.session.SessionManager.Handler;
import net.jibini.cliff.network.session.SessionPlugin;
import net.jibini.cliff.plugin.PluginManager;
import net.jibini.cliff.routing.AsyncPatch;
import net.jibini.cliff.routing.Patch;
import net.jibini.cliff.routing.Request;
import net.jibini.cliff.routing.StitchLink;

public class TestService
{
	private static Logger log = LoggerFactory.getLogger(TestService.class);
	
	private static int read;
	
	private PluginManager manager = PluginManager.create();
	private StitchLink downstream;
	
	private static Object lock;
	
	private static void w() throws InterruptedException
	{
		if (lock != null)
			synchronized (lock)
			{
				if (lock != null)
					lock.wait();
			}
	}
	
	private static void n()
	{
		synchronized (lock)
		{
			lock.notifyAll();
			lock = null;
		}
	}
	
	public static class TestPluginKernel extends SessionKernel
	{
		public TestPluginKernel(Session parent)
		{
			super(parent);
			log.debug("Plugin kernel create . . .");
		}
		
		@Handler("TestRequest")
		public boolean onRequest(Request request)
		{
			log.debug("Request received");
			log.debug(request.toString());
			request.getResponse().put("Hello", "World");
			read++;
			
			return true;
		}
	}
	
	@Before
	public void startPlugin() throws InterruptedException
	{
		read = 0;
		lock = new Object();
		
		SessionPlugin plugin = new SessionPlugin()
		{
			@Override
			public Class<? extends SessionKernel> getKernelClass()
			{
				return TestPluginKernel.class;
			}

			@Override
			public void start()
			{
				log.debug("Plugin start . . .");
			}
		};
		
		JSONObject manifest = new JSONObject();
		manifest.put("name", "Plugin");
		manifest.put("version", "1.0");
		manager.registerPlugin(plugin, manifest);
		manager.notifyPluginStart();
		Thread.sleep(400);
		
		Patch patch = AsyncPatch.create();
		plugin.getSessionManager().getSessionRouter().registerEndpoint("Endpoint", patch.getUpstream());
		downstream = patch.getDownstream();
	}
	
	public static class TestServiceKernel extends SessionKernel
	{
		public TestServiceKernel(Session parent)
		{
			super(parent);
			log.debug("Service kernel create . . .");
		}
		
		@Handler("TestRequest")
		public void onResponse()
		{
			log.debug("Response received");
			read++;
			n();
		}
	}
	
	@Test
	public void testPluginService() throws InterruptedException
	{
		downstream.addPersistentCallback((s, r) ->
		{
			log.debug(r.toString());
		});
		
		Service service = Service.create(downstream, "Plugin", TestServiceKernel.class);
		service.waitSessionCreation();
		service.sendRequest(Request.create("Plugin", "TestRequest"));
		
		w();
		assertEquals("Request callback did not trigger", 2, read);
		read = 0;
	}
}