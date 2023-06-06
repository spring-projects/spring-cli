*Note: The code provided is just an example and may not be suitable for production use.*

Generated on Jun 6, 2023, 11:32:16 AM

Generated using the description: JPA functionality with an integration test.  Include all Java code in the same package.

Sure, here's a sample Spring Boot application that uses Spring Data JPA and includes an integration test. All Java code is included in the same package `com.xkcd.sample.ai.jpa`. The build tool used is Maven.

1. Add the following dependencies to your `pom.xml` file:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

2. Create a `Person` entity with the properties `name` and `phoneNumber`. Generate constructors, property getters, and setters for the entity.

```java
package com.xkcd.sample.ai.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private String phoneNumber;

    public Person() {}

    public Person(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

3. Create a `PersonRepository` interface that extends `JpaRepository<Person, Long>`.

```java
package com.xkcd.sample.ai.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

}
```

4. Create a `PersonService` class that autowires `PersonRepository` and provides a method to save a `Person` object.

```java
package com.xkcd.sample.ai.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PersonService {

    @Autowired
    private PersonRepository personRepository;

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }
}
```

5. Create a `PersonController` class that autowires `PersonService` and provides a REST endpoint to save a `Person` object.

```java
package com.xkcd.sample.ai.jpa;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonController {

    @Autowired
    private PersonService personService;

    @PostMapping("/persons")
    public ResponseEntity<Person> savePerson(@RequestBody Person person) {
        Person savedPerson = personService.savePerson(person);
        return new ResponseEntity<>(savedPerson, HttpStatus.CREATED);
    }
}
```

6. Create an integration test `PersonIntegrationTest` that uses `MockMvc` to test the `PersonController` endpoint.

```java
package com.xkcd.sample.ai.jpa;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@DataJpaTest
@ActiveProfiles("test")
public class PersonIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testSavePerson() throws Exception {
        Person person = new Person("John Doe", "123-456-7890");
        ResultActions resultActions = mockMvc.perform(post("/persons")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonUtil.toJson(person)));
        resultActions.andExpect(status().isCreated());
    }
}
```

7. Create an `application.properties` file in the `src/main/resources` directory with the following content:

```
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=create
```

8. Add the `@SpringBootApplication` annotation to the main application class.

```java
package com.xkcd.sample.ai.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JpaApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpaApplication.class, args);
    }
}
```

Here are some links to tutorials for learning more about Spring Data JPA:

1. https://spring.io/guides/gs/accessing-data-jpa/
2. https://www.baeldung.com/the-persistence-layer-with-spring-data-jpa
3. https://www.javatpoint.com/spring-boot-jpa-hibernate-mysql-example
4. https://www.tutorialspoint.com/spring_boot/spring_boot_data_jpa.htm
5. https://www.callicoder.com/spring-boot-jpa-hibernate-postgresql-restful-crud-api-example/
