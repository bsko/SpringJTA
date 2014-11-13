package ru.jta;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.jta.spring.SomeManager;

public class Bootstrap {
	
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext("ru.jta.spring");
		SomeManager manager = context.getBean(SomeManager.class);
		
		manager.putTestMessageToQueue("Hello Tom!");
		manager.doStuff();
	}
}
