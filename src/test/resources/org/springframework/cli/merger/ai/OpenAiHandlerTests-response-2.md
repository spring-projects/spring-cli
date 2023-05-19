1. Java source code for the Spring Data JPA application:

pom.xml

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
         
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example.restservice.ai.jpa</groupId>
    <artifactId>spring-data-jpa-example</artifactId>
    <version>1.0-SNAPSHOT</version>
    
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

Person.java

```
package com.example.restservice.ai.jpa;

import javax.persistence.*;

@Entity
@Table(name = "person")
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "name")
    private String name;

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
}
```

PersonRepository.java

```
package com.example.restservice.ai.jpa;

import org.springframework.data.repository.CrudRepository;

public interface PersonRepository extends CrudRepository<Person, Long> {
}
```

IntegrationTest.java

```
package com.example.restservice.ai.jpa;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class IntegrationTest {

    @Autowired
    private PersonRepository personRepository;

    @Test
    public void testPersonRepository() {
        Person person = new Person();
        person.setName("John Doe");

        personRepository.save(person);

        Optional<Person> optionalPerson = personRepository.findById(person.getId());

        assertThat(optionalPerson.isPresent()).isTrue();
        assertThat(optionalPerson.get().getName()).isEqualTo(person.getName());
    }
}
```

Application.java

```
package com.example.restservice.ai.jpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
```

2. Tutorials and resources:

- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Data JPA Tutorial: https://www.baeldung.com/spring-data-jpa-tutorial
- Spring Boot Starter Test: https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-testing-spring-boot-applications-testing-dependency-scopes
- Spring Boot Testing: https://www.baeldung.com/spring-boot-testing