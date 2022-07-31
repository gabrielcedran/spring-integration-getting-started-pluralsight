# Spring Integration Study

Spring Integration is a framework that enables easier integration of internal components and external systems of a spring application by
providing the plumbing code* in order to allow developers to focus on what matters - the business logic.

Spring Integration is based on the Enterprise Integration Patterns. Many of the concepts and terminology that Spring Integration uses
come from the EIP.

*Plumbing code is all the code that is necessary to perform a given task and is not related to the business logic and is
repeated over and over again by everybody that needs to do something similar (aka infrastructure). Example: download files from an FTP server.

The code to set up a connection, authenticate, navigate through the server files and download the files are examples
of code that is repeated regardless of the business logic across different applications. Spring Integration focuses on solving this
problem by proving components that perform infra logic so that developers can focus on the business logic. More importantly,
it enabled the separation between integration and business logic.


## What is Integration?

Companies have many software systems and applications to run their business. These systems often have to connect to each other and also connect to system
outside the company itself. Connecting these systems is what Integration means.

These integrations might not always be easy as different systems might have different interfaces and use different data formats.

Some of the ways that systems might be integrated together include web services, messaging systems, exchanging files, shared databases and even custom application level networking protocol.

Integration is about connecting the systems using the mechanisms mentioned above and also translating the data between formats.


## Enterprise Integration Patterns basic concepts and terminology

EIP is all about asynchronous messaging. 

### Message

`Message` is a unit of data that is transmitted to 1 or more receivers via a messaging system. From EIP perspective, a message consists of 2 parts: the payload and the headers.

The payload can be any kind of data and is application specific.

### Message Channel

Messages are transported from the sender to the receivers via a `message channel`. EIP are abstract ideas thus the `message channel` is the concept of something that 
carries the messages from a sender to a receiver but not how specifically it will be done. 

The main two types of message channels are point-to-point (one sender to one receiver, example queues) and publish-subscribe (one sender to multiple receivers, example event notifications).


### Message Endpoint

`Message Enpoint` is the part of the code of the sender and/or receiver that connects to the messaging system - they need to have a way of connecting to the `message channel`.

This is a very general idea and there are more patterns that describe more specific types of message endpoints, such as `channel adapter`, `gateways` and `service activators`.


### Message Transformation

Different system often produce and consume data in different formats (the sender produces JSON but the receiver expects XML). Message Transformation translate 
the messages so that those systems can communicate.

Message transformers can also enrich the message by fetching extra data from another source. EIP describes several different types of message transformers like Message Translator,
Content Enricher, Envelope wrapper, etc.


### Message Routing

There are situations where the receiver of a message might not be fixed and the destination has be decided on the fly (e.g based on the value of a header value). 
There are also several message routing patterns like Message Router, Message Filter, Splitter / Aggregator, etc


## Spring Integration

Controllers calling services directly cause the application to be highly coupled. To reduce the coupling, it is possible to add a channel with a service activator (that is a simple endpoint and one of the EIPs) between the controller and the service and make the controller to send a message via that channel instead:

Controller -> Service
Controller -> Send Message -> Service Activator -> Service


### XML Config

#### Channels

To create channels using xml is pretty straightforward. All that needs to be declared is a channel tag with the channel id:

```
<int:channel id="registrationRequest" />
```

It is possible to choose which type of message channel you want. If you don't, spring will choose one on its own.

#### Service Activators

To create a service activator is as just easy. It requires 3 basic properties: 
1- which input channel it will listen on, 2- the bean it will pass the message on upon message receipt (bean name) and 3- the method of the bean that will be invoked.

```
<int:service-activator
    input-channel="registrationRequest"
    ref="registrationService"
    method="register" />
```

Service activator is a simple type of endpoint that calls some business logic whenever it receives a message on the channel that it is connected to.

#### Sending messages

In a typical spring integration application there are several message channels - developer-defined ones and the ones that spring registers itself so that it can function.

To send a message to a channel, first it is necessary to inject the desired channel on the class that will send the message:

```
    private MessageChannel registrationRequestChannel;

    public RegistrationController(@Qualifier("registrationRequest") MessageChannel registrationRequestChannel) {
        this.registrationRequestChannel = registrationRequestChannel;
    }
```

*** the qualifier is not necessary as long as the name of the bean matches the name of the constructor parameter.

Then a message has to be created with a payload (an way is by using the class message builder):

```
Message<AttendeeRegistration> message = MessageBuilder.withPayload(registration).build();
// There are 2 message builders. 1 under integration package and another under messaging. Favour the second as the first is 
// legacy kept for retrocompatibility purposes.
```

To send the message, just call the method send of the channel with the message:

```
registrationRequestChannel.send(message);
```

### Annotations Config

#### Message Channel

Spring offers a number of message channel types. The first step is to determine the correct type for the use case then declare a new bean using the `@bean` annotation in a `@Configuration` class. The default one is the direct channel:

```
    @Bean
    public MessageChannel registrationRequest() {
        return new DirectChannel();
    } 
```

#### Service Activator

The easiest way to declare a service activator is by annotation the service activator method with the `@ServiceActivator` annotation and provide the `inputChannel` property:

```
@ServiceActivator(inputChannel = "registrationRequest")
```

To instantiate via spring configuration:

```
    @ServiceActivator(inputChannel = "registrationRequest")
    @Bean
    public ServiceActivatingHandler ServiceActivatingHandler(RegistrationService service) {
        return new ServiceActivatingHandler(service, "register");
    }
```


### DSL Config

The domain specific language provides a set of builders with a fluent API to configure spring integration flows in a concise and readable way.

The DSL method requires a integration flow to be defined. The integration flow is a special bean that initialises a set of spring integration components and wire them. 

The integration flow builder (IntegrationFlows) provides an array of methods to allow the flows definition. MessageChannels is also a builder.

```
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
```

This method allows you to clearly specify how components are connected to each other. 

### Message Headers

Adding message headers is just a matter of calling the setHeader method from the message builder:

```
        Message<AttendeeRegistration> message = MessageBuilder
                .withPayload(registration)
                .setHeader("dateTime", OffsetDateTime.now())
                .build();
```

Usually when a service activator passes on the message to a service method, it unpacks the payload from the message and passes it down:

```
    @ServiceActivator(inputChannel = "registrationRequest")
    public void register(AttendeeRegistration registration) {
        ...
    }
```

Two ways of getting the message headers can be seen below - the second should be favoured:

1- Change the argument of the service activator method to receive a message type instead:

```
    @ServiceActivator(inputChannel = "registrationRequest")
    public void register(Message<AttendeeRegistration> message) {
        OffsetDateTime dateTime = (OffsetDateTime) message.getHeaders().get("dateTime");
        AttendeeRegistration registration = message.getPayload();
    }
```

The service activator is smart enough to identify that it shouldn't unpack the message.

*This is the wrong way of getting message headers because the business method shouldn't be aware of messaging system* 
*Other pieces of code that need to use this service would have to pass a message instead of the business object* )

2- (preferred way) receive the necessary business parameters and annotate them to let spring know how to cope with them:

```
    @ServiceActivator(inputChannel = "registrationRequest")
    public void register(@Header("dateTime") OffsetDateTime dateTime, @Payload AttendeeRegistration registration) {
```


### Message Channels Implementations

Spring integration provides many message channel implementations. There are 6 main ones that is important to have a good handle on (the others are really specialised implementation for specific use cases and have to be analised per case).

All channels implement the MessageChannel interface, which contains 2 methods to send messages.

1. Direct Channel

2. Executor Channel

3. Publish Subscribe Channel

4. Queue Channel

5. Rendezvous Channel

6. Priority Channel


The first 3 channels listed above are all Subscribable Channels and the last 3 are pollable channels. 

The distiction between subscribable channels and pollable channels correspond to two enterprise integration patterns: The Event-Driven Consumer pattern (first 3) and the Polling Consumer pattern (last 3).

To receive messages from a subscribable channel, the Event-Driven consumer subscribes a message handler that will be called by the messaging system whenever a message is sent to the channel (the SubscribableChannel interface has 2 methods: `subscribe` and `unsubscribe`).

To receives messages from a pollable channel, you have to explicitly request the message via the receive method (the PollableChannel interface has 2 `receive` methodos. One with and one without timeout). These methods blocks until a message is available (or the timeout occurs).

*Another way of describing the difference between Subscribable and Pollable channels is by defining them as `Pushing` and `Pulling` respectively.*
Another difference is that Subscribable Channels do not buffer messages while Pollable `may` buffer.

#### Subscribable Channels

Direct and Executor channels are unicasting message channels / dispatcher and PublishSubscriber channel is a broadcasting message channel.
This distinction also corresponds to two Enterprise Integration Patterns - Point-to-Point channel and Publish-Subscribe channel. 
The main different might be obvious but the first two deliver the message to exactly one subscribed message handler, thus point-to-point channel. The other one broadcasts messages that are sent to it to all subscribed message handlers.   

_Notice that it is possible to subscribe multiple message handlers to a point-to-point channel however just one will receive the message. Which handler receives the message depends on how you configure the channel._

##### Direct Channel

It is the simplest point-to-point channel implementation that is based on the event-driven consumer pattern. 
The implementation of its send method is super straightforward: the only thing it does is to immediately call one of its subscribers in the same thread that the send method was called.  


##### Executor Channel

Its implementation is very similar to the `Direct Channel's`. The only difference is that it does not call one of its subscribers (message handlers) directly in the same thread as the send method. It calls the subscriber by creating a task on the executor (the executor can be a thread pool, for instance). It allows for asynchronicity (not block the caller).


##### Publish-Subscribe Channel

TBD


#### Pollable Channels

They implement the Polling Consumer pattern. They work by receivers polling messages from the channel and they `may` buffer messages. 
They are always point-to-point channels. Multiple receivers can call the receive method of a pollable channel but each message will be delivered to only one of the receivers.

##### Queue Channel

This is a simple message channel that stores messages in a queue. By default it uses a linked blocking queue with an unbounded capacity.
When receive is called on a queue channel, it gets the next message that is waiting in the queue or it blocks until a sender puts a message in the queue (or a timeout occurs)

##### Rendezvouz Channel

It is similar to a queue channel but with a zero-capacity queue. It means that when a sender sends a message to this type of channel, it will block until the receiver receives the message.

When it is the other way around, when the receiver calls the receive method, if there is no message, the receiver will block until it gets a message.

_The anology of two people agreeing to meet somewhere to deliver a package. Whoever gets there first waits for the other._

##### Priority Channel

It is also similar to the queue channel however it is possible to define priority instead of relying on the first in first out approach.
By default, the priority is set via message header `priority` (:P). But it is also possible to set custom logic to define priority. 

By default queue and priority channel store messages in memory, but it is also possible to make the queues persistent (e.g storing the message in the DB).




##### Miscellaneous

The application runs on port 8080.

To access the db, navigate to `http://localhost:8080/h2-console` and change the url to `jdbc:h2:mem:globomantics`. The credentials is the default one without password.