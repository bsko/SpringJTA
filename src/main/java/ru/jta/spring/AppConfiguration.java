package ru.jta.spring;

import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.SystemException;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.atomikos.icatch.jta.UserTransactionImp;
import com.atomikos.icatch.jta.UserTransactionManager;
import com.atomikos.jdbc.AtomikosDataSourceBean;

import ru.jta.hibernate.EntityDBO;

@EnableTransactionManagement
@Configuration
public class AppConfiguration {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Bean
	public Context context() throws NamingException {
		Hashtable env = new Hashtable();
		env.put("java.naming.factory.initial",
				"org.apache.activemq.jndi.ActiveMQInitialContextFactory");
		env.put("java.naming.provider.url", "tcp://localhost:61616");
		env.put("queue.TestQueue", "example.queue");
		Context ctx = new InitialContext(env);
		return ctx;
	}

	@Bean
	public ConnectionFactory connectionFactory(@Qualifier("context") Context ctx)
			throws NamingException {
		return (ConnectionFactory) ctx.lookup("ConnectionFactory");
	}

	@Bean
	public Queue queue(@Qualifier("context") Context ctx)
			throws NamingException {
		return (Queue) ctx.lookup("TestQueue");
	}

	@Bean
	public JmsTemplate jmsTemplate(
			@Qualifier("connectionFactory") ConnectionFactory connectionFactory,
			@Qualifier("queue") Queue queue) {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.setDefaultDestination(queue);
		template.setReceiveTimeout(3000l);
		template.setSessionTransacted(true);
		return template;
	}
	
	@Bean
	public DataSource dataSource() {
		AtomikosDataSourceBean ds = new AtomikosDataSourceBean();
		ds.setUniqueResourceName("mssql");
		ds.setXaDataSourceClassName("com.microsoft.sqlserver.jdbc.SQLServerXADataSource");
		Properties props = new Properties();
		props.put("URL", "jdbc:sqlserver://localhost;integratedSecurity=true;databaseName=HIBERNATE_TEST");
		ds.setXaProperties(props);
		ds.setPoolSize(3); 
		return ds;
	}
	
	@Bean
	public UserTransactionImp userTransactionImpl() throws SystemException {
		UserTransactionImp userTransaction = new UserTransactionImp();
		userTransaction.setTransactionTimeout(30);
		return userTransaction;
	}
	
	@Bean(initMethod="init", destroyMethod="close")
	public UserTransactionManager userTransactionManagerImpl() {
		return new UserTransactionManager();
	}
	
	@Bean
	public JtaTransactionManager transactionManager(@Qualifier("userTransactionImpl") UserTransactionImp userTransaction, 
			@Qualifier("userTransactionManagerImpl") UserTransactionManager transactionManager) {
		JtaTransactionManager jtatm = new JtaTransactionManager();
		jtatm.setTransactionManager(transactionManager);
		jtatm.setUserTransaction(userTransaction);
		return jtatm;
	}

	@Bean
	public org.hibernate.cfg.Configuration hibernateConfiguration() {
		org.hibernate.cfg.Configuration cfg = new org.hibernate.cfg.Configuration();
		cfg.addClass(EntityDBO.class)
		.setProperty("hibernate.dialect",
				"org.hibernate.dialect.SQLServerDialect")
		.setProperty("hibernate.show_sql", "true")
		.setProperty("hibernate.connection.driver.class",
				"com.microsoft.sqlserver.jdbc.SQLServerDriver")
//		.setProperty(
//				"hibernate.connection.url",
//				"jdbc:sqlserver://localhost;integratedSecurity=true;databaseName=HIBERNATE_TEST")
		.setProperty("hibernate.hbm2ddl.auto", "create");
		return cfg;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Bean
	public SessionFactory sessionFactory(
			@Qualifier("hibernateConfiguration") org.hibernate.cfg.Configuration cfg,
			@Qualifier("dataSource") DataSource dataSource) {
		Map settings = cfg.getProperties();
		settings.put(Environment.DATASOURCE, dataSource);
		ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
				.applySettings(settings).build();
		return cfg.buildSessionFactory(serviceRegistry);
	}

}
