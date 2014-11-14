package ru.jta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ru.jta.spring.CustomException;
import ru.jta.spring.SomeManager;

public class Bootstrap {
	
	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
	
	public static void main(String[] args) {
		@SuppressWarnings("resource")
		ApplicationContext context = new AnnotationConfigApplicationContext("ru.jta.spring");
		SomeManager manager = context.getBean(SomeManager.class);
		try {
			manager.putTestMessageToQueue("Hello Tom!");
			manager.doStuffInOtherWay();
		} catch (CustomException e) {
			logger.error("CustomException", e);
		}
	}
}
