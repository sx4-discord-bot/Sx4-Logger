package com.sx4.bot.handler;

import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ExceptionHandler extends ListenerAdapter {
	
	public void onException(ExceptionEvent event) {
		System.err.println("[Exception]");
		
		event.getCause().printStackTrace();
	}
}