package org.hibernate.orm.test.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hibernate.cache.internal.DefaultCacheKeysFactory;
import org.hibernate.cache.internal.SimpleCacheKeysFactory;
import org.hibernate.cache.spi.CacheKeysFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.orm.test.serialization.entity.PK;
import org.hibernate.orm.test.serialization.entity.WithEmbeddedId;
import org.hibernate.orm.test.serialization.entity.WithSimpleId;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.cache.CachingRegionFactory;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Andrea Boriero
 */
@BaseUnitTest
public class CacheKeySerializationTest {
   private SessionFactoryImplementor getSessionFactory(String cacheKeysFactory) {
      Configuration configuration = new Configuration()
            .setProperty(Environment.USE_SECOND_LEVEL_CACHE, "true")
            .setProperty(Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getName())
            .setProperty(Environment.DEFAULT_CACHE_CONCURRENCY_STRATEGY, "transactional")
            .setProperty("javax.persistence.sharedCache.mode", "ALL")
            .setProperty(Environment.HBM2DDL_AUTO, "create-drop");
      if (cacheKeysFactory != null) {
         configuration.setProperty(Environment.CACHE_KEYS_FACTORY, cacheKeysFactory);
      }
      configuration.addAnnotatedClass( WithSimpleId.class );
      configuration.addAnnotatedClass( WithEmbeddedId.class );
      return (SessionFactoryImplementor) configuration.buildSessionFactory();
   }

   @Test
   @TestForIssue(jiraKey = "HHH-11202")
   public void testSimpleCacheKeySimpleId() throws Exception {
      testId( SimpleCacheKeysFactory.INSTANCE, WithSimpleId.class.getName(), 1L );
   }

   @Test
   @TestForIssue(jiraKey = "HHH-11202")
   public void testSimpleCacheKeyEmbeddedId() throws Exception {
      testId( SimpleCacheKeysFactory.INSTANCE, WithEmbeddedId.class.getName(), new PK( 1L ) );
   }

   @Test
   @TestForIssue(jiraKey = "HHH-11202")
   public void testDefaultCacheKeySimpleId() throws Exception {
      testId( DefaultCacheKeysFactory.INSTANCE, WithSimpleId.class.getName(), 1L  );
   }

   @Test
   @TestForIssue(jiraKey = "HHH-11202")
   public void testDefaultCacheKeyEmbeddedId() throws Exception {
      testId( DefaultCacheKeysFactory.INSTANCE, WithEmbeddedId.class.getName(), new PK( 1L ) );
   }

   private void testId(CacheKeysFactory cacheKeysFactory, String entityName, Object id) throws Exception {
      final SessionFactoryImplementor sessionFactory = getSessionFactory(cacheKeysFactory.getClass().getName());
      final EntityPersister persister = sessionFactory.getRuntimeMetamodels().getMappingMetamodel().getEntityDescriptor(entityName);
      final Object key = cacheKeysFactory.createEntityKey(
            id,
            persister,
            sessionFactory,
            null
      );

      final ByteArrayOutputStream baos = new ByteArrayOutputStream();
      final ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject( key );

      final ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( baos.toByteArray() ) );
      final Object keyClone = ois.readObject();

      try {
         assertEquals( key, keyClone );
         assertEquals( keyClone, key );

         assertEquals( key.hashCode(), keyClone.hashCode() );

         final Object idClone = cacheKeysFactory.getEntityId( keyClone );

         assertEquals( id.hashCode(), idClone.hashCode() );
         assertEquals( id, idClone );
         assertEquals( idClone, id );
         assertTrue( persister.getIdentifierType().isEqual( id, idClone, sessionFactory ) );
         assertTrue( persister.getIdentifierType().isEqual( idClone, id, sessionFactory ) );
         sessionFactory.close();
      }
      finally {
         sessionFactory.close();
      }
   }
}
