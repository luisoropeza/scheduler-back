package com.example.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "scheduler.events";
    public static final String BOOKING_QUEUE = "appointments.booking.notify";
    public static final String BOOKING_ROUTING_KEY = "appointment.booked";

    @Bean
    public TopicExchange schedulerExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue bookingQueue() {
        return QueueBuilder.durable(BOOKING_QUEUE).build();
    }

    @Bean
    public Binding bookingBinding(Queue bookingQueue, TopicExchange schedulerExchange) {
        return BindingBuilder.bind(bookingQueue).to(schedulerExchange).with(BOOKING_ROUTING_KEY);
    }

    @Bean
    public JacksonJsonMessageConverter jsonConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                          JacksonJsonMessageConverter jsonConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonConverter);
        return template;
    }
}
