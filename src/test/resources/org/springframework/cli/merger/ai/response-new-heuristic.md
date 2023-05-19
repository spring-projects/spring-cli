To create a Spring Java application using the Spring Data JPA project, you can follow these steps:

1. Create a new Maven project using your preferred IDE or the command line. You can use the following command to create a new project from the command line:
```
mvn archetype:generate -DgroupId=com.xkcd.sample.ai.jpa -DartifactId=jpa-sample -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

2. Add the following dependencies to your `pom.xml` file:
```xml
<dependencies>
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <!-- H2 Database -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- Spring Boot Starter Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

3. Create a `Person` entity class in the package `com.xkcd.sample.ai.jpa.entity`:
```java
package com.xkcd.sample.ai.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Person {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    
    private String phoneNumber;
    
    public Person() {
        
    }
    
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

4. Create a `PersonRepository` interface in the package `com.xkcd.sample.ai.jpa.repository`:
```java
package com.xkcd.sample.ai.jpa.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.xkcd.sample.ai.jpa.entity.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

}
```

5. Create a `PersonService` class in the package `com.xkcd.sample.ai.jpa.service`:
```java
package com.xkcd.sample.ai.jpa.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xkcd.sample.ai.jpa.entity.Person;
import com.xkcd.sample.ai.jpa.repository.PersonRepository;

@Service
public class PersonService {
    
    @Autowired
    private PersonRepository personRepository;
    
    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
    
    public Person savePerson(Person person) {
        return personRepository.save(person);
    }
    
    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }
}
```

6. Create a `PersonController` class in the package `com.xkcd.sample.ai.jpa.controller`:
```java
package com.xkcd.sample.ai.jpa.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.xkcd.sample.ai.jpa.entity.Person;
import com.xkcd.sample.ai.jpa.service.PersonService;

@RestController
public class PersonController {
    
    @Autowired
    private PersonService personService;
    
    @GetMapping("/persons")
    public List<Person> getAllPersons() {
        return personService.getAllPersons();
    }
    
    @PostMapping("/persons")
    public ResponseEntity<Person> savePerson(@RequestBody Person person) {
        Person savedPerson = personService.savePerson(person);
        return new ResponseEntity<Person>(savedPerson, HttpStatus.CREATED);
    }
    
    @DeleteMapping("/persons/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return new ResponseEntity<Void>(HttpStatus.OK);
    }
}
```

7. Create a `JpaApplication` class in the package `com.xkcd.sample.ai.jpa`:
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

8. Create a test class for the `PersonRepository` interface in the package `com.xkcd.sample.ai.jpa.repository`:
```java
package com.xkcd.sample.ai.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import com.xkcd.sample.ai.jpa.entity.Person;

@DataJpaTest
public class PersonRepositoryTest {
    
    @Autowired
    private PersonRepository personRepository;
    
    @Test
    public void testFindAll() {
        List<Person> persons = personRepository.findAll();
        assertThat(persons.size()).isEqualTo(0);
    }
    
    @Test
    public void testSave() {
        Person person = new Person("John", "123-456-7890");
        Person savedPerson = personRepository.save(person);
        assertThat(savedPerson).isNotNull();
        assertThat(savedPerson.getId()).isNotNull();
    }
    
    @Test
    public void testDeleteById() {
        Person person = new Person("John", "123-456-7890");
        Person savedPerson = personRepository.save(person);
        personRepository.deleteById(savedPerson.getId());
        List<Person> persons = personRepository.findAll();
        assertThat(persons.size()).isEqualTo(0);
    }
}
```

9. Create a test class for the `PersonController` class in the package `com.xkcd.sample.ai.jpa.controller`:
```java
package com.xkcd.sample.ai.jpa.controller;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xkcd.sample.ai.jpa.entity.Person;
import com.xkcd.sample.ai.jpa.service.PersonService;

@WebMvcTest(PersonController.class)
@AutoConfigureMockMvc(addFilters = false)
public class PersonControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @MockBean
    private PersonService personService;
    
    @Test
    public void testGetAllPersons() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/persons"))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
    
    @Test
    public void testSavePerson() throws Exception {
        Person person = new Person("John", "123-456-7890");
        String json = objectMapper.writeValueAsString(person);
        mockMvc.perform(MockMvcRequestBuilders.post("/persons")
            .content(json)
            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("John"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.phoneNumber").value("123-456-7890"));
    }
    
    @Test
    public void testDeletePerson() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/persons/{id}", 1L))
            .andExpect(MockMvcResultMatchers.status().isOk());
    }
}
```

To learn more about Spring Data JPA, you can refer to the following resources:
- https://spring.io/projects/spring-data-jpa
- https://www.baeldung.com/spring-data-jpa-tutorial
- https://www.tutorialspoint.com/spring_data/spring_data_jpa.htm

*Note: The code provided is just an example and may not be suitable for production use.*