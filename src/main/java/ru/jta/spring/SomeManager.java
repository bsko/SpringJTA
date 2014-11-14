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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ru.jta.hibernate.EntityDBO;

@Component
public class SomeManager implements ApplicationContextAware {

	private static final Logger logger = LoggerFactory
			.getLogger(SomeManager.class);

	private JmsTemplate jmsTemplate;
	private SessionFactory sessionFactory;
	private PlatformTransactionManager txManager;

	public void putTestMessageToQueue(final String msg) {
		jmsTemplate.send(new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(msg);
			}
		});
	}
	
	@Transactional(rollbackFor=CustomException.class, propagation=Propagation.REQUIRES_NEW)
	public void doStuffInOtherWay() throws CustomException {
		Message msg = jmsTemplate.receive();
		if (msg != null && msg instanceof TextMessage) {
			TextMessage txtMsg = (TextMessage) msg;
			try {
				logger.info("Data = " + txtMsg.getText());
				EntityDBO entity = new EntityDBO();
				entity.setData(txtMsg.getText());
				org.hibernate.Session session = sessionFactory.getCurrentSession();
				session.save(entity);
//				if (txtMsg.getText().equals("Hello Tom!")) {
//					throw new CustomException();
//				}
			} catch (JMSException e) {
				throw new CustomException();
			}
		} else {
			throw new CustomException();
		}
	}

	public void doStuffInTransaction() throws CustomException {
		TransactionTemplate template = new TransactionTemplate(this.txManager);
		template.execute(new TransactionCallback<Object>() {

			public Object doInTransaction(TransactionStatus status) {
				Message msg = jmsTemplate.receive();
				if (msg != null && msg instanceof TextMessage) {
					TextMessage txtMsg = (TextMessage) msg;
					try {
						logger.info("Data = " + txtMsg.getText());
						if (txtMsg.getText().equals("Hello Tom!")) {
							throw new RuntimeException();
						}
						EntityDBO entity = new EntityDBO();
						entity.setData(txtMsg.getText());
						org.hibernate.Session session = sessionFactory.openSession();
						Transaction tx = session.beginTransaction();
						session.save(entity);
						tx.commit();
					} catch (JMSException e) {
						throw new RuntimeException(e);
					}
				} else {
					throw new RuntimeException();
				}
				return null;
			}
			
		});
	}
	
	public void doStuffSimpleWay() {
		logger.info("Doing stuff..");
		Message msg = jmsTemplate.receive();
		if (msg != null && msg instanceof TextMessage) {
			TextMessage txtMsg = (TextMessage) msg;
			try {
				logger.info("Data = " + txtMsg.getText());
				if (txtMsg.getText().equals("Hello Tom!")) {
					throw new RuntimeException();
				}
				EntityDBO entity = new EntityDBO();
				entity.setData(txtMsg.getText());
				org.hibernate.Session session = sessionFactory.openSession();
				Transaction tx = session.beginTransaction();
				session.save(entity);
				tx.commit();
			} catch (JMSException e) {
				throw new RuntimeException(e);
			}
		} else {
			throw new RuntimeException();
		}
	}

	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.jmsTemplate = applicationContext.getBean(JmsTemplate.class);
		this.sessionFactory = applicationContext.getBean(SessionFactory.class);
		this.txManager = applicationContext.getBean(PlatformTransactionManager.class);
	}

	/**
	 * @return the txManager
	 */
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	/**
	 * @param txManager
	 *            the txManager to set
	 */
	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
}
