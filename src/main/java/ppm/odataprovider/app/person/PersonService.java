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

    public static void changeDepartment(String department){
        List<Person> persons = EntityRepository.findAll(Person.class);
        Person asitha = persons.stream().filter(p -> p.getFirstName().contains("Asitha")).collect(Collectors.toList()).get(0);
        asitha.setDepartment(department);
        EntityRepository.update(asitha);
    }
}
