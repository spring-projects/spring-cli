*Note: The code provided is just an example and may not be suitable for production use.*

Generated on May 26, 2023, 3:40:47 PM

Generated using the description: Please provide instructions for creating a Spring Data MongoDB application, including details on starting a MongoDB server using Docker. Also, include guidance on creating an integration test for the data repository.

To create a Spring Data MongoDB application, follow these steps:

1. Create a new Maven project in your preferred IDE.
2. Add the following dependencies to your pom.xml file:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-mongodb</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

3. Create a new package called "com.xkcd.sample.ai.mongodb".
4. Create a new class called "Person" in the "com.xkcd.sample.ai.mongodb" package. This class will be our domain object. Add the following code to the class:

```java
package com.xkcd.sample.ai.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "people")
public class Person {
    @Id
    private String id;
    private String name;
    private String phoneNumber;

    public Person(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
```

5. Create a new interface called "PersonRepository" in the "com.xkcd.sample.ai.mongodb" package. This interface will be our repository. Add the following code to the interface:

```java
package com.xkcd.sample.ai.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PersonRepository extends MongoRepository<Person, String> {
    Person findByName(String name);
}
```

6. Create a new class called "PersonService" in the "com.xkcd.sample.ai.mongodb" package. This class will be our service layer. Add the following code to the class:

```java
package com.xkcd.sample.ai.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
    @Autowired
    private PersonRepository personRepository;

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    public Person findPersonByName(String name) {
        return personRepository.findByName(name);
    }
}
```

7. Create a new class called "PersonController" in the "com.xkcd.sample.ai.mongodb" package. This class will be our controller layer. Add the following code to the class:

```java
package com.xkcd.sample.ai.mongodb;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/people")
public class PersonController {
    @Autowired
    private PersonService personService;

    @PostMapping
    public Person savePerson(@RequestBody Person person) {
        return personService.savePerson(person);
    }

    @GetMapping("/{name}")
    public Person findPersonByName(@PathVariable String name) {
        return personService.findPersonByName(name);
    }
}
```

8. Create a new file called "application.properties" in the "src/main/resources" directory. Add the following code to the file:

```properties
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=mydb
```

9. Start a MongoDB server using Docker by running the following command in your terminal:

```bash
docker run -d -p 27017:27017 mongo
```

10. Run the application by right-clicking on the main class and selecting "Run As" > "Java Application".
11. Open a web browser and go to "http://localhost:8080/people". You should see an empty array.
12. Use a tool like Postman to send a POST request to "http://localhost:8080/people" with a JSON body containing a name and phone number. You should receive a response with the saved person object.
13. Use a tool like Postman to send a GET request to "http://localhost:8080/people/{name}" with a name parameter. You should receive a response with the person object matching the name parameter.

To create an integration test for the data repository, follow these steps:

1. Create a new class called "PersonRepositoryIntegrationTest" in the "com.xkcd.sample.ai.mongodb" package. Add the following code to the class:

```java
package com.xkcd.sample.ai.mongodb;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
public class PersonRepositoryIntegrationTest {
    @Autowired
    private PersonRepository personRepository;

    @Test
    public void whenFindByName_thenReturnPerson() {
        // given
        Person person = new Person("John Doe", "123-456-7890");
        personRepository.save(person);

        // when
        Person found = personRepository.findByName(person.getName());

        // then
        assertThat(found.getName()).isEqualTo(person.getName());
    }
}
```

2. Run the test by right-clicking on the test class and selecting "Run As" > "JUnit Test". The test should pass.

Links to tutorials for learning more about Spring Data MongoDB:

1. https://spring.io/guides/gs/accessing-data-mongodb/
2. https://www.baeldung.com/spring-data-mongodb-tutorial
3. https://www.javatpoint.com/spring-data-mongodb-tutorial
4. https://www.tutorialspoint.com/spring_data_mongodb/index.htm
5. https://www.mkyong.com/mongodb/spring-data-mongodb-hello-world-example/