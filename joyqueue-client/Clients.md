## 客户端使用说明

### JMQ2 客户端 

JMQ2.0 客户端使用方式参考[java  客户端使用手册](http://jpcloud.jd.com/pages/viewpage.action?pageId=13507053). 配置参数略有不同，详情如下：
- jmq.address: 参考[集群地址](http://help.mq.jd.com/ClusterAddress.html)，填写其中相应集群的nameserver地址
- jmq.app:  同jmq2， 填写app名称。 如果配置了消费者组，则app配置为 "app名称.消费者组"，如"jmq.group1"
- jmq.user:  与jmq.app 相同
- jmq.passwd: 填写jmq4.0 中的token

### JMQ4
客户端demo：http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples

#### pom

配置pom.xml文件如下，joyqueue-client-all必选，其它根据需要引用，具体可参考IDE提示信息进行配置： 
    
```
<!-- 必选 -->
<dependency>
    <groupId>com.jd.joyqueue</groupId>
    <artifactId>joyqueue-client-all</artifactId>
    <version>4.1.0-SNAPSHOT</version>
</dependency>

<!-- 使用spring引用 -->
<dependency>
    <groupId>io.openmessaging</groupId>
    <artifactId>openmessaging-spring</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>

<!-- 使用springboot引用，里面包括openmessaging-spring -->
<dependency>
    <groupId>io.openmessaging</groupId>
    <artifactId>openmessaging-spring-boot-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <exclusions>
        <exclusion>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
        </exclusion>
        <exclusion>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```
#### 手动创建和管理实例

手动创建的使用方式, 可自己控制producer和consumer实例化，自己控制调度等

可参考[手动方式使用](http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/api)

##### accessPoint

首先需要创建MessagingAccessPoint，相当于工厂类，由他再创建producer和consumer

````java
public class SimpleProducer {

    public static void main(String[] args) {
        KeyValue keyValue = OMS.newKeyValue();
        
        // ACCOUNT_KEY对应管理端的令牌，必须填写
        // keyValue还可以传递一些配置，包括超时，重试，namespace等，具体看io.openmessaging.joyqueue.JoyQueueBuiltinKeys
        // 一些配置的默认值可以查看 com.jd.joyqueue.client.internal.producer.config.ProducerConfig, com.jd.joyqueue.client.internal.consumer.config.ConsumerConfig
        keyValue.put(OMSBuiltinKeys.ACCOUNT_KEY, "test_token");
        
        // 创建MessagingAccessPoint, url格式为 oms:joyqueue://[app]@[nameserver]/[region], 三个参数都是必填
        // app对应管理端的应用
        // nameserver根据不同环境填写不同地址，比如jmq.jd.local:80
        // region表示区域，如果使用就近发送就近消费需要填写机房信息，如果不使用填写DEFAULT或UNKNOWN都可以，建议填写真实的
        MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint("oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN", keyValue);

        // 创建producer
        Producer producer = messagingAccessPoint.createProducer();
        producer.start();

        // 生产消息
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        SendResult sendResult = producer.send(message);
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

##### producer

可参考[demo](http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/api/producer/SimpleProducer.java)

生产者，使用MessagingAccessPoint创建，调用start方法后使用

````java
public class SimpleProducer {

    public static void main(String[] args) {
        KeyValue keyValue = OMS.newKeyValue();
        keyValue.put(OMSBuiltinKeys.ACCOUNT_KEY, "test_token");
        MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint("oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN", keyValue);

        // 使用MessagingAccessPoint创建producer
        Producer producer = messagingAccessPoint.createProducer();
        producer.start();

        // 使用producer.createMessage方法创建message
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        
        // 生产消息，不抛异常就算成功，sendResult里的messageId暂时没有意义
        SendResult sendResult = producer.send(message);
        
        // 打印生产结果
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

##### consumer

消费者，使用MessagingAccessPoint创建，调用bindQueue后再调用start使用

可参考[demo](http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/api/consumer/SimpleConsumer.java)

````java
public class SimpleConsumer {

    public static void main(String[] args) {
        KeyValue keyValue = OMS.newKeyValue();
        keyValue.put(OMSBuiltinKeys.ACCOUNT_KEY, "test_token");

        MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint(String.format("oms:joyqueue://test_app@%s:50088/UNKNOWN", IpUtil.getLocalIp()), keyValue);

        // 创建consumer实例
        Consumer consumer = messagingAccessPoint.createConsumer();
        
        // 绑定需要消费的topic和对应的listener
        consumer.bindQueue("test_topic_0", new MessageListener() {
            @Override
            public void onReceived(Message message, Context context) {
                System.out.println(String.format("onReceived, message: %s", message));
                
                // 确认消息消费成功，如果没有确认或抛出异常会进入重试队列
                context.ack();
            }
        });

        consumer.start();
    }
}
````
#### 基本用法

##### producer

基本的生产用法
````java
public class SimpleProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        // 使用producer.createMessage方法创建message
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        
        // 生产消息，不抛异常就算成功，sendResult里的messageId暂时没有意义
        SendResult sendResult = producer.send(message);
        
        // 打印生产结果
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

异步生产
````java
public class AsyncProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        
        Future<SendResult> future = producer.sendAsync(message);
        
        System.out.println(String.format("messageId: %s", future.get().messageId()));
    }
}
````

批量生产
````java
public class BatchProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        List<Message> messages = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            Message message = producer.createMessage("test_topic_0", "body".getBytes());
            messages.add(message);
        }

        producer.send(messages);
    }
}
````

批量异步生产
````java
public class BatchAsyncProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        List<Message> messages = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Message message = producer.createMessage("test_topic_0", "body".getBytes());
            messages.add(message);
        }

        Future<SendResult> future = producer.sendAsync(messages);
        System.out.println(future.get().messageId());
    }
}
````

异步监听生产，异步方法返回的future可以注册listener实现callback
````java
public class BatchAsyncProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        Future<SendResult> future = producer.sendAsync(message);

        future.addListener(new FutureListener<SendResult>() {
            @Override
            public void operationComplete(Future<SendResult> future) {
                System.out.println(future.get().messageId());
            }
        });
    }
}
````

##### consumer

基本的consumer用法
````java
public class SimpleConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) {
        // 绑定需要消费的topic和对应的listener
        consumer.bindQueue("test_topic_0", new MessageListener() {
            @Override
            public void onReceived(Message message, Context context) {
                System.out.println(String.format("onReceived, message: %s", message));
                
                // 确认消息消费成功，如果没有确认或抛出异常会进入重试队列
                context.ack();
            }
        });

        consumer.start();
    }
}
````

批量消费
````java
public class BatchConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) {
        // 绑定需要消费的topic和对应的listener
        consumer.bindQueue("test_topic_0", new BatchMessageListener() {
            @Override
            public void onReceived(List<Message> messages, Context context) {
                for (Message message : messages) {
                    System.out.println(String.format("onReceived, message: %s", message));
                }
                
                // 代表这一批消息的ack
                context.ack();
            }
        });
    
        consumer.start();

        consumer.start();
    }
}
````

##### 事务

jmq4支持事务消息，支持单条事务消息和多条事务消息，未提交的事务消息不会被消费到

单条事务消息
````java
public class TransactionProducer {
    
    private Producder producer;

    public static void main(String[] args) {
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        
        // 添加事务id，设置过事务id的才会被补偿，补偿时会带上这个事务id，非必填
        // 建议根据业务使用有意义的事务id
        message.extensionHeader().get().setTransactionId("test_transactionId");
        
        // 事务prepare
        TransactionalResult transactionalResult = producer.prepare(message);
        
        // 提交事务
        transactionalResult.commit();
        
        // 回滚事务
//        transactionalResult.rollback();

        System.out.println(String.format("messageId: %s", transactionalResult.getMessageId()));
    }
}
````

多条事务消息
````java
public class ExtensionTransactionProducer {
    
    private Producder producer;

    public static void main(String[] args) {
        // 需要先对producer强制转型
        ExtensionProducer extensionProducer = (ExtensionProducer) producer;
        
        ExtensionTransactionalResult transactionalResult = extensionProducer.prepare();
        
        for (int i = 0; i < 10; i++) {
            // 可以发多条事务消息
            Message message = extensionProducer.createMessage("test_topic_0", "body".getBytes());
            
            // 添加事务id，设置过事务id的才会被补偿，补偿时会带上这个事务id，非必填
            // 建议根据业务使用有意义的事务id
            message.extensionHeader().get().setTransactionId("test_transactionId");
            
            SendResult sendResult = transactionalResult.send(message);
            System.out.println(sendResult.messageId());
        }

        // 提交事务
        transactionalResult.commit();
        
        // 回滚事务
//        transactionalResult.rollback();
    }
}
````

##### 事务补偿

事务补偿，在超时时间内没有进行commit或rollback的事务会通过回调的方式补偿，由producer确认后commit或rollback

只有生产时指定了transactionId的消息才会被补偿

简单补偿例子
````java
public class TransactionProducer {
    
    private Producder producer;

    public static void main(String[] args) {
        // 创建producer，如果是spring或springboot通过xml和注解方式添加
        // producer只会补偿发送过的主题的事务
        Producer producer = messagingAccessPoint.createProducer(new TransactionStateCheckListener() {
            @Override
            public void check(Message message, TransactionalContext context) {
                // 使用context的commit和rollback方法提交事务状态
                // 使用transactionId进行状态查询，这里只是简单例子，以具体业务为准
                String topic = message.header().getDestination();
                String transactionId = message.extensionHeader().get().getTransactionId();
                Xxx xxx = xxxService.findXxxById(transactionId);
                
                // 如果是已完成状态，代表业务完成，那么提交事务
                if (xxx.getState().equals(SUCCESS)) {
                    context.commit();
                } else {
                    // 否则进行回滚或重试业务逻辑再次提交
                    context.rollback();
//                    context.commit();
                }
            }
        });

        // 发送事务消息
        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        message.extensionHeader().get().setTransactionId("test_transactionId");
        
        TransactionalResult transactionalResult = producer.prepare(message);
        transactionalResult.commit();

        System.out.println(String.format("messageId: %s", transactionalResult.messageId()));
    }
}
````

#### 高级用法

##### 元数据相关

可以通过producer和consumer获取到简单的元数据，根据元数据进行调度和分配

元数据例子
````java
public class SimpleMetadata {

    public static void main(String[] args) {
        KeyValue keyValue = OMS.newKeyValue();
        keyValue.put(OMSBuiltinKeys.ACCOUNT_KEY, "test_token");

        MessagingAccessPoint messagingAccessPoint = OMS.getMessagingAccessPoint(String.format("oms:joyqueue://test_app@%s:50088/UNKNOWN", IpUtil.getLocalIp()), keyValue);

        Producer producer = messagingAccessPoint.createProducer();
        producer.start();

        QueueMetaData queueMetaData = producer.getQueueMetaData("test_topic_0");
        for (QueueMetaData.Partition partition : queueMetaData.partitions()) {
            System.out.println(String.format("partition: %s, partitionHost: %s", partition.partitionId(), partition.partitonHost()));
        }

        Message message = producer.createMessage("test_topic_0", "body".getBytes());

        SendResult sendResult = producer.send(message);
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

##### producer

producer生产可以指定partition

````java
public class SimpleProducer {
    
    private Producer producer;

    public static void main(String[] args) {
        // 使用producer.createMessage方法创建message
        Message message = producer.createMessage("test_topic_0", "body".getBytes());

        // 设置messageKey，和jmq2的businessId相同，非必填
        // 如果需要相对顺序消息，也可以使用messageKey作为key指定分区
        message.extensionHeader().get().setMessageKey("test_key");
        
        // 自定义发送的partition
        // 最好根据元数据分配，不要写死partitions
        List<QueueMetaData.Partition> partitions = producer.getQueueMetaData("test_topic_0").partitions();
        QueueMetaData.Partition partition = partitions.get((int) System.currentTimeMillis() % partitions.size());
        message.extensionHeader().get().setPartition(partition.partitionId());

        // 生产消息，不抛异常就算成功，sendResult里的messageId暂时没有意义
        SendResult sendResult = producer.send(message);

        // 打印生产结果
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

##### consumer

consumer可以由客户端调度拉取消息，也可以由用户自行调度拉取消息，也可以由用户自己指定想要拉取的partition和index

单条拉取调度
````java
public class ReceiveConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) throws Exception {
        // 绑定主题，将要消费的主题
        consumer.bindQueue("test_topic_0");

        // 这里只是简单例子，根据具体情况进行调度处理
        while (true) {
            // 拉取单条消息
            // 参数是超时时间，只是网络请求的超时
            Message message = consumer.receive(1000 * 10);

            // 没有拉取到，继续循环
            if (message == null) {
                continue;
            }

            // 拉取到消息，打印日志并ack
            System.out.println(String.format("receive, message: %s", message));
            consumer.ack(message.getMessageReceipt());

            Thread.currentThread().sleep(1000 * 1);
        }
    }
}
````

批量拉取调度
````java
public class BatchReceiveConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) throws Exception {
        // 绑定主题，将要消费的主题
        consumer.bindQueue("test_topic_0");

        // 这里只是简单例子，根据具体情况进行调度处理
        // 参数是超时时间，只是网络请求的超时
        while (true) {
            // 批量拉取消息
            List<Message> messages = consumer.batchReceive(1000 * 10);
            
            if (CollectionUtils.isEmpty(messages)) {
                continue;
            }
    
            for (Message message : messages) {
                // 拉取到消息，打印日志并ack
                System.out.println(String.format("receive, message: %s", messages));
                consumer.ack(message.getMessageReceipt());
            }

            Thread.currentThread().sleep(1000 * 1);
        }
    }
}
````

拉取调度和指定partition
````java
public class PartitionReceiveConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) throws Exception {
        // 首先需要对consumer强制转型
        ExtensionConsumer extensionConsumer = (ExtensionConsumer) consumer;
        
        // 绑定主题，将要消费的主题
        extensionConsumer.bindQueue("test_topic_0");

        // 获取元数据
        QueueMetaData queueMetaData = extensionConsumer.getQueueMetaData("test_topic_0");

        // 这里只是简单例子，根据具体情况进行调度处理
        while (true) {
            // 循环所有partition，并拉取相应消息
            for (QueueMetaData.Partition partition : queueMetaData.partitions()) {
                // 拉取分区单条消息
                // 参数是超时时间，只是网络请求的超时
                Message message = extensionConsumer.receive((short) partition.partitionId(), 1000 * 10);
                
                // 批量拉取的方式相同，不单独列出
//                List<Message> messages = extensionConsumer.batchReceive((short) partition.partitionId(), 1000 * 10);
    
                // 没有拉取到，继续循环
                if (message == null) {
                    continue;
                }
    
                // 拉取到消息，打印日志并ack
                System.out.println(String.format("receive, message: %s", message));
                extensionConsumer.ack(message.getMessageReceipt());
            }

            Thread.currentThread().sleep(1000 * 1);
        }
    }
}
````

拉取调度和指定partition和index
````java
public class PartitionIndexReceiveConsumer {
    
    private Consumer consumer;

    public static void main(String[] args) throws Exception {
        // 首先需要对consumer强制转型
        ExtensionConsumer extensionConsumer = (ExtensionConsumer) consumer;
                
        // 绑定主题，将要消费的主题
        extensionConsumer.bindQueue("test_topic_0");

        // 获取元数据
        QueueMetaData queueMetaData = extensionConsumer.getQueueMetaData("test_topic_0");
        Map<Integer, Long> indexMapper = Maps.newHashMap();

        // 这里只是简单例子，根据具体情况进行调度处理
        while (true) {
            // 循环所有partition，并拉取相应消息
            for (QueueMetaData.Partition partition : queueMetaData.partitions()) {
                // 初始化index信息
                Long index = indexMapper.get(partition.partitionId());
                if (index == null) {
                    index = 0L;
                    indexMapper.put(partition.partitionId(), index);
                }
                                
                // 拉取分区单条消息
                // 参数是超时时间，只是网络请求的超时
                Message message = extensionConsumer.receive((short) partition.partitionId(), index, 1000 * 10);
                
                // 批量拉取的方式相同，不单独列出
//                List<Message> messages = extensionConsumer.batchReceive((short) partition.partitionId(), index, 1000 * 10);
    
                // 没有拉取到，继续循环
                if (message == null) {
                    continue;
                }
    
                // 拉取到消息，打印日志并ack
                System.out.println(String.format("receive, message: %s", message));
                extensionConsumer.ack(message.getMessageReceipt());
                
                // 更新index到下一个消息处
                index = message.extensionHeader().get().getOffset() + 1;
                indexMapper.put(partition.partitionId(), index);
            }

            Thread.currentThread().sleep(1000 * 1);
        }
    }
}
````

##### 拦截器

拦截器分为producer和consumer两种，可以实现日志，监控，消息过滤等

下面的例子都是基于api方式的，如果是spring或springboot可以使用xml和注解声明

拦截器分为openmessaging和jmq两种，openmessaging只有基本的拦截器，jmq额外提供消息过滤，拦截器排序等

consumer拦截器只有在使用listener时生效

简单consumer拦截器 (openmessaging)
````java
public class SimpleConsumerInterceptor {
    
    private Consumer consumer;

    public static void main(String[] args) throws Exception {
        consumer.addInterceptor(new ConsumerInterceptor() {
            @Override
            public void preReceive(Message message, Context context) {
                // 消费前执行
                System.out.println(String.format("preReceive, message: %s", message));
            }

            @Override
            public void postReceive(Message message, Context context) {
                // 消费后执行
                System.out.println(String.format("postReceive, message: %s", message));
            }
        });

        consumer.bindQueue("test_topic_0", new MessageListener() {
            @Override
            public void onReceived(Message message, Context context) {
                System.out.println(String.format("onReceived, message: %s", message));
                context.ack();
            }
        });
    }
}
````

简单consumer拦截器 (jmq)

使用时需要通过spi的方式注册
把实现类添加到 META-INF/services/com.jd.joyqueue.client.internal.consumer.interceptor.ConsumerInterceptor 文件内，每行一个

````java
// Ordered接口提供getOrder方法，用于指定顺序，可以不实现
// context还有attributes等可使用，具体看com.jd.joyqueue.client.internal.consumer.interceptor.ConsumeContext
public class JMQSimpleConsumerInterceptor implements ConsumerInterceptor, Ordered {

    @Override
    public boolean preConsume(ConsumeContext context) {
        System.out.println("preConsume");
        
        // 循环一批消息，单条和批消息都是按批拦截
        for (ConsumeMessage message : context.getMessages()) {
            // 过滤消息
            context.filterMessage(message);
        }
        
        // 返回true表示这批消息可以消费，返回false表示这批消息不可消费
        return true;
    }

    @Override
    public void postConsume(ConsumeContext context, List<ConsumeReply> consumeReplies) {
        System.out.println("postConsume");
    }

    @Override
    public int getOrder() {
        // 值小的先执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
````

简单producer拦截器 (openmessaging)
````java
public class SimpleProducerInterceptor {
    
    private Producer producer;

    public static void main(String[] args) {
        producer.addInterceptor(new ProducerInterceptor() {
            @Override
            public void preSend(Message message, Context attributes) {
                // 发送前执行
                System.out.println(String.format("preSend, message: %s", message));
            }

            @Override
            public void postSend(Message message, Context attributes) {
                // 发送后执行
                System.out.println(String.format("postSend, message: %s", message));
            }
        });

        producer.start();

        Message message = producer.createMessage("test_topic_0", "body".getBytes());
        SendResult sendResult = producer.send(message);
        System.out.println(String.format("messageId: %s", sendResult.messageId()));
    }
}
````

简单producer拦截器 (jmq)

使用时需要通过spi的方式注册
把实现类添加到 META-INF/services/com.jd.joyqueue.client.internal.producer.interceptor.ProducerInterceptor 文件内，每行一个

````java
// Ordered接口提供getOrder方法，用于指定顺序，可以不实现
// context还有attributes等可使用，具体看com.jd.joyqueue.client.internal.producer.interceptor.ProduceContext
public class JMQSimpleProducerInterceptor implements ProducerInterceptor, Ordered {

    @Override
    public boolean preSend(ProduceContext context) {
        System.out.println("preSend");

        // 循环一批消息，单条生产和批量生产都是按批拦截
        for (ProduceMessage message : context.getMessages()) {
        }

        // 返回true表示这批消息可以生产，返回false表示这批消息不可生产
        return true;
    }

    @Override
    public void postSend(ProduceContext context, List<SendResult> result) {
        System.out.println("postSend");
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
````

拦截器的具体使用例子

可参考

[简单按环境消息生产和过滤](http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/api/interceptor)
[客户端监控](http://git.jd.com/laf/laf-jmq/tree/master/jmq-client/jmq-client-core/src/main/java/com/jd/jmq/client/internal/trace/interceptor)

##### 监控

客户端内置监控接口和ump的实现，根据需求使用

客户端内置Trace接口，用于实现监控，目前监控包括生产，消费，拉取消息三种

可参考

[ump监控](http://git.jd.com/laf/laf-jmq/tree/master/jmq-jd/jmq-client-ump/src/main/java/com/jd/jmq/client/internal/ump)

ump的实现提供可配参数，可以通过com.jd.joyqueue.client.internal.ump.UMPTraceConfig的appNames设置监控的app，如果有需要可自行设置

#### spring

和spring整合的使用方式

可参考[demo](http://git.jd.com/laf/JournalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/spring)

````xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:oms="http://openmessaging.io/schema"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://openmessaging.io/schema
        http://openmessaging.io/schema/oms.xsd">
    
    <!-- 定义accessPoint -->
    <oms:access-point url="oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN">
        <oms:attribute key="ACCOUNT_KEY" value="test_token"></oms:attribute>
        <!-- 更多配置 -->
    </oms:access-point>

    <!-- 创建producer实例，由spring创建并管理生命周期，不需要再手动调用start -->
    <oms:producer id="producer1"></oms:producer>
    
    <!-- 需要需要事务补偿，可以指定对应的listener，需要实现io.openmessaging.producer.TransactionStateCheckListener接口 -->
    <oms:producer id="producer2" listener="com.jd.joyqueue.client.samples.spring.SimpleTransactionStateCheckListener"></oms:producer>

    <!-- 如果listener不需要由spring自动创建，也可以设置listener的引用 -->
    <bean id="simpleTransactionStateCheckListener" class="com.jd.joyqueue.client.samples.spring.SimpleTransactionStateCheckListener"></bean>
    <oms:producer id="producer3" listener-ref="simpleTransactionStateCheckListener"></oms:producer>
    
    <!-- 创建一个consumer实例，由spring创建并管理生命周期，不需要再手动调用bind和start -->
    <!-- queue-name对应需要消费的主题 -->
    <!-- listener需要实现io.openmessaging.consumer.MessageListener或io.openmessaging.consumer.BatchMessageListener接口，对应单条和批量消费 -->
    <oms:consumer queue-name="test_topic_0" listener="com.jd.joyqueue.client.samples.spring.SimpleMessageListener"></oms:consumer>
    
    <!-- 如果listener不需要由spring自动创建，也可以设置listener的引用 -->
    <bean id="messageListenerRef" class="com.jd.joyqueue.client.samples.spring.SimpleMessageListener"></bean>
    <oms:consumer queue-name="test_topic_1" listener-ref="messageListenerRef"></oms:consumer>
    
    <!-- 定义拦截器，如果不需要由spring自动创建，也可以设置interceptor的引用 -->
    <!-- 需要实现io.openmessaging.interceptor.ProducerInterceptor或io.openmessaging.interceptor.ConsumerInterceptor接口，对应生产和消费拦截 -->
    <oms:interceptor class="com.jd.joyqueue.client.samples.spring.SimpleConsumerInterceptor"></oms:interceptor>
    <!--<oms:interceptor ref="consumerInterceptor1"></oms:interceptor>-->
</beans>
````

如果需要配置多套access-point，可以给access-point添加id，在producer和consumer上指定不同的access-point

````xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:oms="http://openmessaging.io/schema"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
        http://www.springframework.org/schema/beans/spring-beans.xsd
        http://openmessaging.io/schema
        http://openmessaging.io/schema/oms.xsd">
    
    <!-- accessPoint1 -->
    <oms:access-point id="accessPoint1" url="oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN">
        <oms:attribute key="ACCOUNT_KEY" value="test_token"></oms:attribute>
    </oms:access-point>

    <oms:producer id="producer1" access-point="accessPoint1"></oms:producer>

    <oms:consumer access-point="accessPoint1" queue-name="test_topic_0" listener="com.jd.joyqueue.client.samples.spring.SimpleMessageListener"></oms:consumer>

    <!-- accessPoint2 -->
    <oms:access-point id="accessPoint2" url="oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN">
        <oms:attribute key="ACCOUNT_KEY" value="test_token"></oms:attribute>
    </oms:access-point>

    <oms:producer id="producer2" access-point="accessPoint2"></oms:producer>

    <oms:consumer access-point="producer2" queue-name="test_topic_0" listener="com.jd.joyqueue.client.samples.spring.SimpleMessageListener"></oms:consumer>
</beans>
````

#### spring-boot

和spring-boot整合的使用方式

可参考[demo](http://git.jd.com/laf/journalQ/tree/master/joyqueue-client/joyqueue-client-samples/src/main/java/com/jd/joyqueue/client/samples/springboot)

##### 配置

````properties
#spring.oms.url是核心配置，必须配置，否则无法使用
#spring.oms.attributes是一些配置信息，里面的ACCOUNT_KEY必须配置，对应管理端的app
spring.oms.url=oms:joyqueue://test_app@jmq.jd.local:80/UNKNOWN
spring.oms.attributes[ACCOUNT_KEY]=test_token

#是否启用消费者, 默认启用
#spring.oms.consumer.enable=false

#是否启用生产者，默认启用
#spring.oms.producer.enable=false

#是否启用拦截器，默认启用
#spring.oms.interceptor.enable=false

#是否启用事务补偿，默认启用
#spring.oms.producer.transaction.check.enable=false
````

##### producer

默认配置下springboot会自动创建一个producer，注入到bean中可直接使用，不需要start

````java
@SpringBootApplication
@ComponentScan("com.jd.joyqueue.client.samples.springboot")
public class SpringBootMain implements InitializingBean {

    protected static final Logger logger = LoggerFactory.getLogger(SpringBootMain.class);

    // 注入producer实例
    @Resource
    private Producer producer;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMain.class);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        for (int i = 0; i < 10; i++) {
            Message message = producer.createMessage("test_topic_0", "test".getBytes());
            SendResult sendResult = producer.send(message);
            logger.info("sendResult: {}", sendResult);
        }
    }
}
````

##### consumer

consumer需要使用io.openmessaging.spring.boot.annotation.OMSMessageListener注解定义

分为两种使用方式，一种是通过类实现接口的方式，另一种是通过方法直接消费

实现接口
````java
@Component // 注册到spring
@OMSMessageListener(queueName = "test_topic_0") // 声明类一个consumer，并消费test_topic_0
// 也可以实现io.openmessaging.consumer.BatchMessageListener批量消费
public class SimpleMessageListener implements MessageListener {

    @Override
    public void onReceived(Message message, MessageListener.Context context) {
        System.out.println(String.format("receive, message: %s", message));
        context.ack();
    }
}
````

方法消费，方法参数不能写错
````java
@Component // 注册到spring
public class SimpleMessageListener {

    @OMSMessageListener(queueName = "test_topic_0") // 指定方法消费test_topic_0
    public void onReceived(Message message, MessageListener.Context context) {
        System.out.println(String.format("receive, message: %s", message));
        context.ack();
    }

    @OMSMessageListener(queueName = "test_topic_1") // 指定方法批量消费test_topic_1
    public void onReceived(List<Message> messages, BatchMessageListener.Context context) {
        for (Message message : messages) {
            System.out.println(String.format("receive, message: %s", message));
        }
        context.ack();
    }
}
````

##### 事务补偿

事务补偿也是通过注解声明

````java
@OMSTransactionStateCheckListener // 声明是一个补偿类，已包含Component注解，可直接被spring扫描
public class SimpleTransactionStateCheckListener implements TransactionStateCheckListener {

    @Override
    public void check(Message message, TransactionalContext context) {
        System.out.println(String.format("check, message: %s", message));
        context.commit();
    }
}
````

##### 拦截器

拦截器也是通过注解声明

````java
@OMSInterceptor // 声明是一个拦截器，已包含Component注解，可直接被spring扫描
// 可以实现ProducerInterceptor或ConsumerInterceptor接口
public class SimpleProducerInterceptor implements ProducerInterceptor {

    @Override
    public void preSend(Message message, Context attributes) {
        System.out.println(String.format("preSend, message: %s", message));
    }

    @Override
    public void postSend(Message message, Context attributes) {
        System.out.println(String.format("postSend, message: %s", message));
    }
}
````

 
### kafka

- jmq兼容kafka协议，可直接使用原生kafka客户端, 但使用kafka-python客户端时需在配置中指定客户端版本为'0.9.0.1' （正在开发，后续不需要指定）

#### kafka-java客户端

和kafka使用方式一样，唯一区别需要指定group.id和client.id为app

kafka客户端建议使用1.0.0版本以上，兼容0.9及以上版本，不兼容0.8及以下版本

pom
````java
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
    <version>2.1.0</version>
</dependency>
````

producer
````java
public class SimpleKafkaProducer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "jmq.jd.local:80");
        
        // 需指定client.id，值是管理端的app
        // 如果特殊情况需要多个producer实例，可以在app后加 '-' 区分，比如 test_app-0,test_app-0
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "test_app");
        
        // 其他配置根据实际情况配置，这里的只是演示
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(props);

        kafkaProducer.send(new ProducerRecord<String, String>("test_topic_0", "test"));
    }
}
````

consumer
````java
public class SimpleKafkaConsumer {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfigs.BOOTSTRAP);
        
        // 需指定group.id，值是管理端的app
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConfigs.GROUP_ID);
        
        // 需指定client.id，值是管理端的app
        // 如果特殊情况需要多个producer实例，可以在app后加 '-' 区分，比如 test_app-0,test_app-0
        props.put(ConsumerConfig.CLIENT_ID_CONFIG, KafkaConfigs.GROUP_ID);
        
        // 其他配置根据实际情况配置，这里只是演示
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test_topic_0"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000 * 1));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println(String.format("record, key: %s, value: %s, offset: %s", record.key(), record.value(), record.offset()));
            }
        }
    }
}
````

#### kafka-python客户端
- git 地址：https://github.com/dpkp/kafka-python
- 使用说明：
    0. 注意事项：
        - jmq兼容kafka 客户端1.0.0版本的协议
        - 生产时如果有异常需要根据业务逻辑做异常处理，重新发送消息
        - 支持发送到指定key和partition
        - 压缩方式建议使用gzip。其它压缩方式中，lz4和snappy服务端支持，但客户端配置较复杂，zstd服务端不支持
    1. 安装：
        
        ```
              >>> pip install kafka-python
        ```
        
    2. 发送消息：
          
        ```python
            from kafka import KafkaProduce
            topic="your_topic"
            message="test_message"
            conf={
                'bootstrap_servers':'test-nameserver.jmq.jd.local:50088',
                'client_id':'your_app'
            }
            producer = KafkaProducer(**conf)
            try:
                future=producer.send(your_topic,message)
                result = future.get(timeout=60)
            except BaseException as e:
                # 发送失败时，用户需根据业务逻辑做异常处理，否则消息可能会丢失
                print(e)
        ```
        
    3. 消费消息
    
        ```python
        from kafka import KafkaConsumer

        conf={
            'bootstrap_servers':'test-nameserver.jmq.jd.local:50088',
            'client_id':'your_app',
            'api_version':'0.9.0.1'
        }
        conf['group_id'] = 'your_app.your_group'
        consumer = KafkaConsumer(your_topic,**conf)      
        for msg in consumer:
            print(msg)
        ```

### MQTT

#### MQTT开发与测试注意事项

* 可以参阅网上其它关于mqtt的教程来完善自己的mqtt业务场景开发。
* JMQ4 MQTT服务器只支持MQTT Version 3.1.1协议版本。
* JMQ4 MQTT服务器连接connection报文必须提供鉴权信息：username和password，username为应用名称，password为应用token。
* JMQ4 MQTT服务支持clean session客户端。
* JMQ4 MQTT服务不支持will主题和消息，也不支持retained消息投递。
* JMQ4 MQTT服务器目前不支持qos=2的消息交互协议，只能发送qos<=1的报文，订阅主题没有变化，因为消息发送都是qos<=1，所以即使订阅qos=2的主题即会投递qos=1的消息，整体消息服务为AT_MOST_ONCE和AT_LEAST_ONCE两种。
* JMQ4 MQTT测试服务器地址为192.168.53.169，192.168.131.206，192.168.131.205，连接其中任意一台即可，如果有问题请尝试更换地址再做测试。
* JMQ4 MQTT线上服务器，请参考<a href ="http://git.jd.com/laf/laf-jmq/wikis/JMQ4%E6%8E%A5%E5%85%A5(%E7%BA%BF%E4%B8%8A&%E6%B5%8B%E8%AF%95)">Jmq4接入(线上&测试)</a>服务文档，线上JMQ4 MQTT服务连接域名即可，外网连接地址：mqtt.jd.com:2000，内网连接地址：mqtt.jd.local:1883。

#### MQTT开发用例参考
在开发测试前，推荐使用一些开源的mqtt client工具来做测试，服务器端兼容并且支持开源客户端连接的，比如GUI支持很好的MQTT.fx，或者其他使用paho库的客户端实现。如要开发和使用mqtt客户端与服务器的交互，来满足自己的业务场景，以下为使用paho这个开源客户端实现为例进行说明：

```
<dependency>
    <groupId>org.eclipse.paho</groupId>
    <artifactId>org.eclipse.paho.client.mqttv3</artifactId>
    <version>1.1.1</version>
</dependency>
```

```java
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_3_1_1;

/**
 * @author majun8
 */
public class MqttTest {
    private static final Logger logger = LoggerFactory.getLogger(MqttTest.class);

    private static String topic = "mqtt_test";
    private static String address = "tcp://ip_address:1883";
    private static String username = "mqtt_test";
    private static String password = "xxxxxx-xxxx-xxxx-xxxx-xxxxxxx";
    private static String constantClientId = "mqtt_client_test_majun";
    private static MqttClient client;
    private static MqttConnectOptions connectOptions = new MqttConnectOptions();

    static {
        try {
            String clientId = constantClientId + "-" + UUID.randomUUID().toString().replace("-", "");
            client = new MqttClient(address, clientId, new MemoryPersistence());
            client.setTimeToWait(1000);
            client.setCallback(new MqttCallback() {

                public void connectionLost(Throwable cause) {
                    logger.error("connectionLost-----------: <{}>", cause);
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    logger.info("deliveryComplete-----------: <{}>", token);
                }

                public void messageArrived(String topic, MqttMessage arg1)
                        throws Exception {
                    logger.info("messageArrived-----------: <{}>, <{}>", topic, arg1.toString());

                }
            });

            connectOptions.setCleanSession(false);
            connectOptions.setUserName(username);
            connectOptions.setPassword(password.toCharArray());
            connectOptions.setConnectionTimeout(30);
            connectOptions.setKeepAliveInterval(60);
            connectOptions.setMqttVersion(MQTT_VERSION_3_1_1);
            connectOptions.setMaxInflight(100);

            client.connect(connectOptions);
        } catch (Exception e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testPublish() throws MqttException {
        String messagebody = "123-test";
        logger.info("messagebody is " + messagebody);
        MqttMessage message = new MqttMessage();
        message.setQos(1);
        message.setPayload(messagebody.getBytes(Charset.forName("UTF-8")));
        client.publish(topic, message);
    }

    public void testSubscribe() throws MqttException {
        client.subscribe(topic, 1);
    }

    public void testUnSubscribe() throws MqttException {
        client.unsubscribe(topic);
    }

    public void disconnect() throws MqttException {
        client.disconnect();
        client.close();
    }

    public static void main(String args[]) {
    // new MqttTest().testPublish();
	// new MqttTest().testSubscribe();
	// new MqttTest().testUnSubscribe();
	// new MqttTest().disconnect();
    }

}
```

测试用例中mqtt客户端使用了回调函数MqttCallback，这里paho的客户端实现都是异步的，所以最好使用该回调函数来跟踪客户端操作后的状态与信息，比如连接是否断开connectionLost，发送publish消息是否成功deliveryComplete，以及订阅subscribe后是否收到消息messageArrived。

#### MQTT开发与测试说明

* MQTT代理集群，目前JMQ4 MQTT测试环境为docker部署3台，分别为：192.168.53.169，192.168.131.206，192.168.131.205，测试环境没有配置域名，请使用mqtt客户端连接其中任意一台服务器即可进行测试，端口号为默认的mqtt协议1883配置。
* ClientID请使用自己独立的并且唯一的标识ID（推荐业务应用名称加随机数生成组合），以避免跟其他客户端ID冲突而导致一些协议交互错误。如果使用相同ClientID的多个客户端进行连接，服务器会受理最新连入的相同ClientID客户端，断开之前连入受理的客户端，请知悉，如果设置了重连情况下出现频繁的连接成功并中断连接很可能是这种情况，请尝试更换另一个唯一的ClientID进行客户端连接。
* Connect timeout设置，客户端连接超时默认可以设置30秒，该超时时间设置针对客户端连接服务器时服务器的最大响应时间，如果超过该值设置的时间内没有连接成功可能有多种原因，可能是网络原因，地址端口不正确或者服务器连接数上限无响应，如必要请连接管理员处理。
* KeepAlive时间设置，该设置是mqtt客户端与服务器之间的心跳包ping-pong交互时间，每个keepalive时间间隔客户端都会发送ping报文给服务器，服务器收到该clientID的ping报文即可发送pong响应报文，以此作为客户端判断服务器存活的关键，相反如果服务器在keepalive时间间隔的1.5倍时间内没有收到ping请求报文，服务器也会判断该客户端是否存活，做出响应的处理，即客户端端和服务器端都会主动断开连接。所以请根据客户端的场景合理设置该值，该值默认可以设置为60秒。
* CleanSession设置，该值是布尔数值，表示该客户端是否session持久化。true的情况为在客户端失去与服务器的连接后（主动断开与被动断开），服务器不保留客户端的订阅关系，客户端再次连接后需要做订阅关系的操作才能消费，并且消费的消息为最新推送的消息。false的情况为持久化session，服务器会保证客户端的订阅关系，在任何时刻再次连接后都会立即推送之前持久化的消息，保证消息不丢失。
* Max Inflight设置，该值体现在发送消息的客户端发送窗口大小，针对qos>0的消息，发送到服务器后必须等待服务器的响应，qos=1和2的响应与处理交互是不一样，但都会在没收到服务器确认该消息回执的同时占用这个发送窗口，直到收到回执后才会释放该窗口的消息数量保证其他消息可被发送，该值默认可以设置为10，如果发送qos>0的消息并且单位时间内要求发送消息吞吐高的情况下请尽量增大该窗口的值，比如50或者100。
* MQTT version设置，请设置该协议版本为3.1.1，目前JMQ4 MQTT服务只支持3.1.1的连接受理，如果使用3.1及以下版本的话在连接交互的过程中会提示：Invalid protocol version。
* JMQ4 MQTT服务必须要求鉴权，所以username和password必须配置，不能为空，username的值配置成申请创建的应用名称，password的值配置成申请创建的应用令牌。如果配置不正确或者鉴权导致的问题在连接交互的过程中会提示：Bad user name or password。
* JMQ4 MQTT服务同时支持SSL/TLS安全加密通信，目前只提供CA certificate file加密方式，如需证书秘钥文件及相关帮助，请联系管理员。
* JMQ4 MQTT服务也提供mqtt over websocket支持，可以使用http协议连接后升级到websocket协议上，然后使用mqtt报文进行操作交互，后续业务流程一致，mqtt over websocket适合浏览器客户端的场景，有额外的js库mqtt client可以开发该应用业务实现，适合一些移动端http浏览器或者轻量级js客户端的实现。