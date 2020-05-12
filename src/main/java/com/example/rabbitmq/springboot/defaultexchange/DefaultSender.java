package com.example.rabbitmq.springboot.defaultexchange;

import com.example.rabbitmq.springboot.RmConst;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 *类说明：
 */
@Component
public class DefaultSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void send(String msg) {
        String sendMsg = msg +"---"+ System.currentTimeMillis();;
        System.out.println("Sender : " + sendMsg);
        // 普通消息发送
        this.rabbitTemplate.convertAndSend(RmConst.QUEUE_HELLO, sendMsg);
        // 发送手动应答消息
//        this.rabbitTemplate.convertAndSend(RmConst.QUEUE_USER, sendMsg);
    }

}
