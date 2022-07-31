package com.pluralsight.demo;

import com.pluralsight.demo.service.RegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class GlobomanticsIntegrationConfig {

    @Bean
    public MessageChannel registrationRequest() {
        return new DirectChannel();
    }

    @ServiceActivator(inputChannel = "registrationRequest")
    @Bean
    public ServiceActivatingHandler ServiceActivatingHandler(RegistrationService service) {
        return new ServiceActivatingHandler(service, "register");
    }

}

