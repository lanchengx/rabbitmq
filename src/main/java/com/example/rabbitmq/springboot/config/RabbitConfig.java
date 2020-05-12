package com.example.rabbitmq.springboot.config;

import com.example.rabbitmq.springboot.RmConst;
import com.example.rabbitmq.springboot.defaultexchange.UserReceiver;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**

 *类说明：
 */
@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.host}")
    private String addresses;

    @Value("${spring.rabbitmq.port}")
    private String port;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    @Value("${spring.rabbitmq.publisher-confirm-type}")
    private CachingConnectionFactory.ConfirmType publisherConfirms;

    @Autowired
    private UserReceiver userReceiver;

    /**
     * 连接工厂
     * @return
     */
    @Bean
    public ConnectionFactory connectionFactory() {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
        connectionFactory.setAddresses(addresses+":"+port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        /** 如果要进行消息回调，则这里必须要设置为true */
        connectionFactory.setPublisherConfirmType(publisherConfirms);
        return connectionFactory;
    }

    /**
     * rabbit Admin 封装对rabbit的管理权限
     * @param connectionFactory
     * @return
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
        return new RabbitAdmin(connectionFactory);
    }

    /**
     * 使用template
     * @return
     */
    @Bean
    public RabbitTemplate newRabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        // 失败通知
        template.setMandatory(true);
        // 发送方确认 监听
        template.setConfirmCallback(confirmCallback());
        // 失败回调
        template.setReturnCallback(returnCallback());
        return template;
    }

    //===============使用了RabbitMQ系统缺省的direct交换器==========
    /**
     * todo 申明队列（最简单的方式）  rabbitmq 中申明队列时默认一个direct交换器
     * RmConst.QUEUE_HELLO 为绑定路由键  绑定键即为队列名称
     * @return Queue
     */
    @Bean
    public Queue helloQueue() {
        return new Queue(RmConst.QUEUE_HELLO);
    }
    @Bean
    public Queue userQueue() {
        return new Queue(RmConst.QUEUE_USER);
    }

    //=========================以下是验证topic Exchange====================
    /**
     *  申明topic 交换器
     * @return
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(RmConst.EXCHANGE_TOPIC);
    }

    /**
     * 申明队列
     * @return
     */
    @Bean
    public Queue queueEmailMessage() {
        return new Queue(RmConst.QUEUE_TOPIC_EMAIL);
    }
    @Bean
    public Queue queueUserMessages() {
        return new Queue(RmConst.QUEUE_TOPIC_USER);
    }

    /**
     * 绑定交换器和队列
     * @return
     */
    @Bean
    public Binding bindingEmailExchangeMessage() {
        return BindingBuilder
                .bind(queueEmailMessage())
                .to(topicExchange())
                .with("sb.*.email");
    }
    @Bean
    public Binding bindingUserExchangeMessages() {
        return BindingBuilder
                .bind(queueUserMessages())
                .to(topicExchange())
                .with("sb.*.user");
    }




    //===============以下是验证Fanout Exchange==========

    /**
     * fanout交换器
     * @return
     */
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange(RmConst.EXCHANGE_FANOUT);
    }

    /**
     * 申明队列
     * @return
     */
    @Bean
    public Queue AMessage() {
        return new Queue("sb.fanout.A");
    }

    /**
     * 绑定fanout 交换器
     * @param AMessage
     * @param fanoutExchange
     * @return
     */
    @Bean
    Binding bindingExchangeA(Queue AMessage,FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(AMessage).to(fanoutExchange);
    }


    //=======================消费者确认=========================

    /**
     * 手动应答
     * @return
     */
    @Bean
    public SimpleMessageListenerContainer messageContainer() {
        SimpleMessageListenerContainer container
                = new SimpleMessageListenerContainer(connectionFactory());
        // 绑定 sb.user 使用手动应答
        container.setQueues(userQueue());
        // 手动提交
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        // 发送方确认
        container.setMessageListener(userReceiver);
        return container;
    }

    //======================生产者发送方法=========================

    /**
     * 生产者确认
     * @return
     */
    @Bean
    public RabbitTemplate.ConfirmCallback confirmCallback(){
        return new RabbitTemplate.ConfirmCallback(){

            @Override
            public void confirm(CorrelationData correlationData,
                                boolean ack, String cause) {
                if (ack) {
                    System.out.println("发送者确认发送给mq成功");
                } else {
                    //处理失败的消息
                    System.out.println("发送者发送给mq失败,考虑重发:"+cause);
                }
            }
        };
    }

    /**
     * 失败者通知
     * @return
     */
    @Bean
    public RabbitTemplate.ReturnCallback returnCallback(){
        return new RabbitTemplate.ReturnCallback(){

            @Override
            public void returnedMessage(Message message,
                                        int replyCode,
                                        String replyText,
                                        String exchange,
                                        String routingKey) {
                System.out.println("无法路由的消息，需要考虑另外处理。");
                System.out.println("Returned replyText："+replyText);
                System.out.println("Returned exchange："+exchange);
                System.out.println("Returned routingKey："+routingKey);
                String msgJson  = new String(message.getBody());
                System.out.println("Returned Message："+msgJson);
            }
        };
    }

}
