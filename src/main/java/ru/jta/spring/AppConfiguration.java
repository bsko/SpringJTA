package ru.jta.spring;

import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

import ru.jta.hibernate.EntityDBO;

@Configuration
public class AppConfiguration {
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public Context context() throws NamingException {
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		env.put("java.naming.provider.url", "tcp://localhost:61616");
		env.put("queue.TestQueue", "example.queue");
		Context ctx = new InitialContext(env);
		return ctx;
	}
	
	@Bean
	public ConnectionFactory connectionFactory(@Qualifier("context") Context ctx) throws NamingException {
		return (ConnectionFactory) ctx.lookup("ConnectionFactory");
	}
	
	@Bean
	public Queue queue(@Qualifier("context") Context ctx) throws NamingException {
		return (Queue) ctx.lookup("TestQueue");
	}
	
	@Bean
	public JmsTemplate jmsTemplate(@Qualifier("connectionFactory") ConnectionFactory connectionFactory, 
			@Qualifier("queue") Queue queue) {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setDefaultDestination(queue);
		template.setReceiveTimeout(3000l);
		return template;
	}
	
	@Bean
	public org.hibernate.cfg.Configuration hibernateConfiguration() {
		org.hibernate.cfg.Configuration cfg = new org.hibernate.cfg.Configuration();
		cfg.addClass(EntityDBO.class)
		.setProperty("hibernate.dialect", "org.hibernate.dialect.SQLServerDialect")
		.setProperty("hibernate.show_sql", "true")
		.setProperty("hibernate.connection.driver.class", "com.microsoft.sqlserver.jdbc.SQLServerDriver")
		.setProperty("hibernate.connection.url", "jdbc:sqlserver://localhost;integratedSecurity=true;databaseName=HIBERNATE_TEST")
		.setProperty("hibernate.hbm2ddl.auto", "create");
		return cfg;
	}
	
	@Bean 
	public SessionFactory sessionFactory(@Qualifier("hibernateConfiguration") org.hibernate.cfg.Configuration cfg) {
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(cfg.getProperties()).build();
		return cfg.buildSessionFactory(serviceRegistry);
	}

}
