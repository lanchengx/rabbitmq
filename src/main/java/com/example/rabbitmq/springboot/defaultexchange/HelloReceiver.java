package com.example.rabbitmq.springboot.defaultexchange;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 监听sb.hello  最简单消费方式  默认自动应答
 *类说明：
 */
@Component
@RabbitListener(queues = "sb.hello")
public class HelloReceiver {

    @RabbitHandler
    public void process(String hello) {
        System.out.println("HelloReceiver : " + hello);
    }

}
