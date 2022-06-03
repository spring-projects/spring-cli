# Basic Spring Data JPA application

This project contains a test class that exercises the Spring Data JPA APIs for a `Customer` entity.

The class `CustomerRepository` extends the `CrudRepository` that provides basic create, retrieve, update and delete functionality for `Customer` entities.

The test class `CustomerRepositoryTests` exercises the repository by persisting a new `Customer` and then retrieving it.

All data is persisted in an in memory H2 database.

```java
Customer customer = new Customer("first", "last");
entityManager.persist(customer);

List<Customer> findByLastName = customers.findByLastName(customer.getLastName());
```

If you run the application with the `test-data` profile, a few `Customer` entries will be created.

## Running the code

To run the test and exercise the `CustomerRepository` functionality in a test

```bash
./mvnw clean package
``` 





