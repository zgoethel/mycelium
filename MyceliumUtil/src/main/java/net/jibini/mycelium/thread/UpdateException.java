package net.jibini.mycelium.thread;

public class UpdateException extends RuntimeException
{
	public UpdateException(String message) { super(message); }
	
	public UpdateException(String message, Throwable t) { super(message, t); }
	
	public UpdateException(Throwable t) { super(t); }
}
