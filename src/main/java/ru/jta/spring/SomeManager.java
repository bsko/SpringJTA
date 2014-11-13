package ru.jta.spring;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

import ru.jta.hibernate.EntityDBO;

@Component
public class SomeManager implements ApplicationContextAware {
	
	private static final Logger logger = LoggerFactory.getLogger(SomeManager.class);
	
	private JmsTemplate jmsTemplate;
	private SessionFactory sessionFactory;
	
	public void putTestMessageToQueue(final String msg) {
		jmsTemplate.send(new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msg);
			}
		});
	}
	
	public void doStuff() {
		logger.info("Doing stuff..");
		Message msg = jmsTemplate.receive();
		if(msg != null && msg instanceof TextMessage) {
			TextMessage txtMsg = (TextMessage)msg;
			try {
				logger.info("Data = " + txtMsg.getText());
				if(txtMsg.getText().equals("Hello Tom!")) {
					throw new RuntimeException();
				}
				EntityDBO entity = new EntityDBO();
				entity.setData(txtMsg.getText());
				org.hibernate.Session session = sessionFactory.openSession();
				Transaction tx = session.beginTransaction();
				session.save(entity);
				tx.commit();
			} catch(JMSException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.jmsTemplate = applicationContext.getBean(JmsTemplate.class);
		this.sessionFactory = applicationContext.getBean(SessionFactory.class);
	}
}
