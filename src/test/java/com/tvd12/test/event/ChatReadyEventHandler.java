package com.tvd12.test.event;

import com.tvd12.test.annotation.ServerEventHandler;

public class ChatReadyEventHandler {
	
	@ServerEventHandler
	public static class ChatReadyA {
	}
	
	@ServerEventHandler
	public static class ChatReadyB {
	}
	
}
