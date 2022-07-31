package com.pluralsight.demo;

import com.pluralsight.demo.service.RegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class GlobomanticsIntegrationConfig {

    @Bean
    public MessageChannel registrationRequest() {
        return MessageChannels.direct("registrationRequest").get();
    }

    @Bean
    public IntegrationFlow integrationFlow(RegistrationService registrationService) {
        return IntegrationFlows
                .from("registrationRequest")
                .handle(registrationService, "register")
                .get();
    }

}

