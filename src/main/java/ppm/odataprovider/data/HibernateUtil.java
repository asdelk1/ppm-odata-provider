package ppm.odataprovider.data;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import ppm.odataprovider.app.task.Task;
import ppm.odataprovider.app.person.Person;


public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try{
            Configuration configuration = new Configuration();
            //TODO: need to fetch mapping class by looking at a es.json file
            configuration.addAnnotatedClass(Task.class);
            configuration.addAnnotatedClass(Person.class);
            return configuration.buildSessionFactory(new StandardServiceRegistryBuilder().build());
        }catch(Exception e){
            e.printStackTrace();
            System.err.println(e.getMessage());
            throw new RuntimeException("Error creating hibernate session factory");
        }
    }

    public static SessionFactory getSessionFactory(){
        return sessionFactory;
    }

}
