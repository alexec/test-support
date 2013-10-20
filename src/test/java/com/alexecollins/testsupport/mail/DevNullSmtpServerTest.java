package com.alexecollins.testsupport.mail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/testContext.xml")
public class DevNullSmtpServerTest {

	@Autowired
	JavaMailSenderImpl javaMailSender;
	private DevNullSmtpServer server;

	@Before
	public void setUp() throws Exception {
		server = new DevNullSmtpServer();
		server.start();
	}

	@After
	public void tearDown() throws Exception {
		server.close();
	}

	@Test
	public void sentMailIsSent() throws Exception {

		final SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom("release-manager@alexecollins.com");
		message.setTo("alex@alexecollins.com");
		message.setSubject("Test Email");
		message.setText("Test Text");
		javaMailSender.send(message);


	}
}
