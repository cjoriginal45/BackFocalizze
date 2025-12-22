package com.focalizze.Focalizze;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class FocalizzeApplicationTests {

    // Mockeamos el JavaMailSender para que no intente conectarse a un servidor SMTP real
    // y para que Spring encuentre un bean de este tipo al levantar el contexto.
    @MockitoBean
    private JavaMailSender javaMailSender;
	@Test
	void contextLoads() {
	}

}
