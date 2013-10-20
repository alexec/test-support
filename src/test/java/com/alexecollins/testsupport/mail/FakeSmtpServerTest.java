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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/testContext.xml")
public class FakeSmtpServerTest {

	@Autowired
	JavaMailSenderImpl javaMailSender;
	private FakeSmtpServer server;

	@Before
	public void setUp() throws Exception {
		server = new FakeSmtpServer();
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
		message.setTo(new String[] {"alex@alexecollins.com","alex.e.c@gmail.com"});
		message.setSubject("Test Email");
		message.setText("Test Text\nline 1");
		javaMailSender.send(message);

		assertEquals(message.getFrom(), server.lastMessage().getFrom());
		assertArrayEquals(message.getTo(), server.lastMessage().getTo());
		assertEquals(message.getSubject(), server.lastMessage().getSubject());
		assertEquals(message.getText(), server.lastMessage().getText());

	}
}
