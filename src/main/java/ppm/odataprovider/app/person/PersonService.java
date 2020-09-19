package ppm.odataprovider.app.person;

import ppm.odataprovider.data.EntityRepository;
import ppm.odataprovider.data.PpmODataGenericService;

import java.util.List;
import java.util.stream.Collectors;

public class PersonService extends PpmODataGenericService {

    public static List<Person> getFreePersons(String personName){
        List<Person> persons = EntityRepository.findAll(Person.class);
        return persons.stream().filter(p -> p.getFirstName().contains(personName)).collect(Collectors.toList());
    }
}
