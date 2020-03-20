package ppm.odataprovider.data;

import org.hibernate.Query;
import org.hibernate.Session;

import java.util.List;

public class DataRepository {

    public static <T> List<T> getAll(Class clazz){
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        String baseQuery = String.format("from %s", clazz.getName());
        Query query = session.createQuery(baseQuery);
        List<T> entityList = query.list();
        session.getTransaction().commit();
        session.close();
        return entityList;
    }

    public static <T, K> T get(Class clazz, String keyName, K key){
        T entity = null;
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        String baseQuery = String.format("from %s t WHERE t.%s = %s", clazz.getName(), keyName, key);
//        String baseQuery = "from " + clazz.getName() + " WHERE ";
        Query query = session.createQuery(baseQuery);
        List<T> entityList = query.list();
        if(!entityList.isEmpty()){
            entity = entityList.get(0);
        }
        session.getTransaction().commit();
        session.close();
        return entity;
    }

    public static <T> T save(Class clazz, T entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Long id = (Long) session.save(entity);
        T createdEntity = (T) session.get(clazz, id);
        session.getTransaction().commit();
        session.close();
        return createdEntity;
    }

}
