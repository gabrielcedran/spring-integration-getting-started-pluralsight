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



### DSL Config


##### Miscellaneous

The application runs on port 8080.

To access the db, navigate to `http://localhost:8080/h2-console` and change the url to `jdbc:h2:mem:globomantics`. The credentials is the default one without password.