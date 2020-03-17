package ppm.odataprovider.data;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import ppm.odataprovider.app.task.Task;


public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try{
            Configuration configuration = new Configuration();
            configuration.addAnnotatedClass(Task.class);
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

    public static String getTableName(Class entityClass){
        return entityClass.getName();
    }

//    public static String getIdColumns(Class entityClass){
//        for(Field field : entityClass.getDeclaredFields()){
//            Annotation[] annotations =  field.getAnnotations();
//            if(Arrays.stream(annotations).anyMatch(annotation -> { annotation.getClass().getName() == "Id"})){
//
//            }
//        }
//    }
}
