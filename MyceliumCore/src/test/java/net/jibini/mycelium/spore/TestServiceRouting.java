package net.jibini.mycelium.spore;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.jibini.mycelium.Mycelium;
import net.jibini.mycelium.api.Interaction;
import net.jibini.mycelium.api.InternalRequest;
import net.jibini.mycelium.api.Request;
import net.jibini.mycelium.api.RequestEvent;
import net.jibini.mycelium.event.Handles;
import net.jibini.mycelium.hook.Hook;
import net.jibini.mycelium.network.NetworkAdapter;

public class TestServiceRouting
{
	private String verboseNet = "false";
	
	public static class TestInteraction implements Interaction
	{
		@Override
		public Interaction spawn()
		{ return new TestInteraction(); }
		
		@Handles("TestRequest")
		public void testRequest(RequestEvent event)
		{ event.echo(); }
		
		
		@Handles("TestRequest2")
		public void testRequest2(RequestEvent event)
		{
			event.request().body().put("value", "Foo Bar");
			event.source().send(event.request());
		}
	}
	
	@Before
	public void startMycelium() throws InterruptedException
	{ 
		verboseNet = (String)System.getProperties().getOrDefault("verboseNetworking", "false");
		System.setProperty("verboseNetworking", "true");
		
		Mycelium.main(new String[0]);
		
		Thread.sleep(500);
	}
	
	private static final SporeProfile testProfile = new SporeProfile()
			{
				@Override
				public String serviceName()
				{ return "TestSpore"; }

				@Override
				public String version()
				{ return "1.0"; }

//				@Override
//				public int protocolVersion()
//				{ return 1; }
			};
			
	public static class TestSpore extends AbstractSpore
	{
		private NetworkAdapter net;
		
		public TestSpore(NetworkAdapter net)
		{ this.net = net; }

		@Override
		public SporeProfile profile()
		{ return testProfile; }

		@Hook(Spore.HOOK_UPLINK)
		public void postUplink()
		{ interactions().registerStartPoint("TestRequest", new TestInteraction()); }
		

		@Hook(Spore.HOOK_SERVICE_AVAILABLE)
		public void postServiceAvailable()
		{
			try
			{
				Thread.sleep(200);
			} catch (InterruptedException ex)
			{  }
			
			Request send = new InternalRequest()
					.withTarget("TestSpore")
					.withRequest("TestRequest");
			send.body().put("value", "Hello, world!");
			net.send(send);
		}
		
	}

	@Test(timeout=2500)
	public void testUplinkEcho() throws InterruptedException
	{
		NetworkAdapter net = new NetworkAdapter()
				.connect("127.0.0.1", 25605);

		new Thread(() ->
		{
			new TestSpore(net).start();
		}).start();
		
//		Thread.sleep(200);
		
		Request response = net.read();
		assertEquals("Hello, world!", response.body().getString("value"));

		response.header().put("request", "TestRequest2");
		net.send(response);
		
		Request response2 = net.read();
		assertEquals("Foo Bar", response2.body().getString("value"));
	}
	
	@After
	public void closeMycelium() throws InterruptedException
	{
		Mycelium.SPORE.close();
		
		System.setProperty("verboseNetworking", verboseNet);
		Thread.sleep(500);
	}
}
