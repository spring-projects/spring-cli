# Basic Spring Config Client application

This project demonstrates the use of the Spring Configuration Server client.

A Controller named `MessageRestController` contains a controller with an `@Value` annotation whose value can be sourced from the Spring Configuration server


```java
@RestController
public class MessageRestController {

	@Value("${message:Hello default}")
	private String message;

	@RequestMapping("/message")
	public String getMessage() {
		return this.message;
	}
}
```

## Building and running

```bash
./mvnw spring-boot:run
```

Invoking `curl http://localhost:8080/message` will display the message.

The `application.properties` file contains the configuration to connect to a Configuration server.
