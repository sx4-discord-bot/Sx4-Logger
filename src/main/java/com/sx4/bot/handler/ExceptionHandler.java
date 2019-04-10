package com.sx4.bot.handler;

import java.time.LocalDateTime;

import com.sx4.bot.Sx4Logger;

import net.dv8tion.jda.core.events.ExceptionEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class ExceptionHandler extends ListenerAdapter {
	
	public void onException(ExceptionEvent event) {
		System.err.println("[" + LocalDateTime.now().format(Sx4Logger.getTimeFormatter()) + "] [onException]");
		
		event.getCause().printStackTrace();
	}
}