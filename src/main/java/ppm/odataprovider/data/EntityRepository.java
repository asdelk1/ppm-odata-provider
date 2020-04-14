package ppm.odataprovider.data;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EntityRepository<T> {

    public static <T> List<T> findAll(Class entityClazz) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        Criteria criteria = session.createCriteria(entityClazz);
        List<T> entityList = criteria.list();
        session.getTransaction().commit();
//        session.close();
        return entityList;
    }

    public static <T> Optional<List<T>> find(Class entityClazz, Map<String, Object> params) {
        Optional<List<T>> result = Optional.empty();
        Session session = HibernateUtil.getSessionFactory().openSession();
        Criteria criteria = session.createCriteria(entityClazz);
        params.forEach((k, v) -> criteria.add((Restrictions.eq(k, v))));
        List<T> resultList = criteria.list();
        if (!resultList.isEmpty()) {
            result = Optional.of(resultList);
        }
//        session.close();
        return result;
    }

    public static <T> Optional<T> get(Class entityClazz, String id) {
        Optional<T> result = Optional.empty();
        Session session = HibernateUtil.getSessionFactory().openSession();
        T entity = (T) session.get(entityClazz, id);
        if (entity != null) {
            result = Optional.of(entity);
        }
//        session.close();
        return result;
    }

    public static <T> T save(T entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        String id = (String) session.save(entity);
        transaction.commit();
        T createdEntity = (T) session.get(entity.getClass(), id);
        session.close();
        return createdEntity;
    }

    public static <T> void update(T entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.update(entity);
        transaction.commit();
        session.close();
    }

    public static <T> void delete(T entity) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction transaction = session.beginTransaction();
        session.delete(entity);
        transaction.commit();
        session.close();
    }
}
