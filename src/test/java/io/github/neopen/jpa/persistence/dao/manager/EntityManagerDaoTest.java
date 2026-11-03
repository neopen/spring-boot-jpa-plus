package io.github.neopen.jpa.persistence.dao.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EntityManagerDaoTest {

    @Test
    @SuppressWarnings("unchecked")
    void hqlUsesOneBasedPositionalParameters() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        TypedQuery<String> query = mock(TypedQuery.class);
        when(entityManager.createQuery("select p.name from Product p where p.type = ?1 and p.status = ?2", String.class))
                .thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList("keyboard"));

        EntityManagerHqlDao dao = new EntityManagerHqlDao();
        ReflectionTestUtils.setField(dao, "em", entityManager);

        List<String> result = dao.findAll(
                "select p.name from Product p where p.type = ?1 and p.status = ?2",
                String.class, "device", "ACTIVE");

        assertEquals(Arrays.asList("keyboard"), result);
        verify(query).setParameter(1, "device");
        verify(query).setParameter(2, "ACTIVE");
    }

    @Test
    void nativeCountAcceptsAnyNumberImplementation() throws Exception {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("select count(*) from orders")).thenReturn(query);
        when(query.getSingleResult()).thenReturn(123L);

        EntityManagerSqlDao dao = new EntityManagerSqlDao();
        ReflectionTestUtils.setField(dao, "em", entityManager);

        assertEquals(123L, dao.count("select count(*) from orders"));
    }
    @Test
    void nativeUpdateUsesOneBasedPositionalParameters() {
        EntityManager entityManager = mock(EntityManager.class);
        Query query = mock(Query.class);
        when(entityManager.createNativeQuery("update users set status = ?1 where id = ?2")).thenReturn(query);
        when(query.executeUpdate()).thenReturn(1);

        EntityManagerSqlDao dao = new EntityManagerSqlDao();
        ReflectionTestUtils.setField(dao, "em", entityManager);

        int updated = dao.executeUpdate(
                "update users set status = ?1 where id = ?2",
                "DISABLED", 7L);

        assertEquals(1, updated);
        verify(query).setParameter(1, "DISABLED");
        verify(query).setParameter(2, 7L);
    }

}
