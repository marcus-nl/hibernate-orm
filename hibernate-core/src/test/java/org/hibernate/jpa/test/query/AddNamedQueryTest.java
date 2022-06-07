/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.query;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.engine.spi.NamedQueryDefinition;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.jpa.test.Distributor;
import org.hibernate.jpa.test.Item;
import org.hibernate.jpa.test.Wallet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link javax.persistence.EntityManagerFactory#addNamedQuery} handling.
 *
 * @author Steve Ebersole
 */
public class AddNamedQueryTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[]{
				Item.class,
				Distributor.class,
				Wallet.class
		};
	}

	@Test
	public void basicTest() {
		// just making sure we can add one and that it is usable when we get it back
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from Item" );
		final String name = "myBasicItemQuery";
		em.getEntityManagerFactory().addNamedQuery( name, query );
		Query query2 = em.createNamedQuery( name );
		query2.getResultList();
		em.close();
	}

	@Test
	public void criteriaQueryTest() {
		EntityManager em = getOrCreateEntityManager();
		
		em.getTransaction().begin();
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Item> query = cb.createQuery( Item.class );
		Root<Item> root = query.from( Item.class );
		final String name = "myCriteriaItemQuery";
		em.getEntityManagerFactory().addNamedQuery( name, em.createQuery(query) );

		em.getTransaction().begin();
		TypedQuery<Item> query2 = em.createNamedQuery( name, Item.class );
		item = query2.getSingleResult();
		assertEquals( "Mouse", item.getName() );
		assertEquals( "Micro$oft mouse", item.getDescr() );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void tupleCriteriaQueryTest() {
		EntityManager em = getOrCreateEntityManager();
		
		em.getTransaction().begin();
		Item item = new Item( "Mouse", "Micro$oft mouse" );
		em.persist( item );
		assertTrue( em.contains( item ) );
		em.getTransaction().commit();

		em.getTransaction().begin();
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Tuple> query = cb.createQuery( Tuple.class );
		Root<Item> root = query.from( Item.class );
		query.multiselect(root.get("name").alias("n"), root.get("descr").alias("d"));
		final String name = "myCriteriaItemQuery";
		TypedQuery<Tuple> query2 = em.createQuery(query);
		Tuple tuple = query2.getSingleResult();
		assertEquals( "Mouse", tuple.get( "n" ) );
		assertEquals( "Micro$oft mouse", tuple.get( "d" ) );
		em.getEntityManagerFactory().addNamedQuery( name, query2 );
		em.getTransaction().commit();

		em.getTransaction().begin();
		query2 = em.createNamedQuery( name, Tuple.class );
		tuple = query2.getSingleResult();
		assertEquals( "Mouse", tuple.get( "n" ) );
		assertEquals( "Micro$oft mouse", tuple.get( "d" ) );
		em.getTransaction().commit();

		em.close();
	}

	@Test
	public void replaceTest() {
		EntityManager em = getOrCreateEntityManager();
		final String name = "myReplaceItemQuery";

		// create a jpql query
		String sql = "from Item";
		Query query = em.createQuery( sql );
		query.setHint( "org.hibernate.comment", sql );
		em.getEntityManagerFactory().addNamedQuery( name, query );
		query = em.createNamedQuery( name );
		assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
		assertEquals( 0, query.getResultList().size() );

		// create a native query and replace the previous jpql
		sql = "select * from Item";
		query = em.createNativeQuery( sql, Item.class );
		query.setHint( "org.hibernate.comment", sql );
		em.getEntityManagerFactory().addNamedQuery( name, query );
		query = em.createNamedQuery( name );
		assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
		assertEquals( 0, query.getResultList().size() );

		// define back a named query
		sql = "from Item";
		query = em.createQuery( sql );
		query.setHint( "org.hibernate.comment", sql );
		em.getEntityManagerFactory().addNamedQuery( name, query );
		query = em.createNamedQuery( name );
		assertEquals( sql, query.getHints().get( "org.hibernate.comment" ) );
		assertEquals( 0, query.getResultList().size() );

		em.close();
	}

	@Test
	public void testLockModeHandling() {
		final String name = "lock-mode-handling";

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query q = em.createQuery( "from Item" );
		assertEquals( LockModeType.NONE, q.getLockMode() );
		q.setLockMode( LockModeType.OPTIMISTIC );
		assertEquals( LockModeType.OPTIMISTIC, q.getLockMode() );
		em.getEntityManagerFactory().addNamedQuery( name, q );

		// first, lets check the underlying stored query def
		SessionFactoryImplementor sfi = entityManagerFactory().unwrap( SessionFactoryImplementor.class );
		NamedQueryDefinition def = sfi.getNamedQueryRepository().getNamedQueryDefinition( name );
		assertEquals( LockMode.OPTIMISTIC, def.getLockOptions().getLockMode() );

		// then lets create a query by name and check its setting
		q = em.createNamedQuery( name );
		assertEquals( LockMode.OPTIMISTIC, q.unwrap( org.hibernate.Query.class ).getLockOptions().getLockMode() );
		assertEquals( LockModeType.OPTIMISTIC, q.getLockMode() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testFlushModeHandling() {
		final String name = "flush-mode-handling";

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query q = em.createQuery( "from Item" );
		assertEquals( FlushModeType.AUTO, q.getFlushMode() );
		q.setFlushMode( FlushModeType.COMMIT );
		assertEquals( FlushModeType.COMMIT, q.getFlushMode() );
		em.getEntityManagerFactory().addNamedQuery( name, q );

		// first, lets check the underlying stored query def
		SessionFactoryImplementor sfi = entityManagerFactory().unwrap( SessionFactoryImplementor.class );
		NamedQueryDefinition def = sfi.getNamedQueryRepository().getNamedQueryDefinition( name );
		assertEquals( FlushMode.COMMIT, def.getFlushMode() );

		// then lets create a query by name and check its setting
		q = em.createNamedQuery( name );
		assertEquals( FlushMode.COMMIT, q.unwrap( org.hibernate.Query.class ).getHibernateFlushMode() );
		assertEquals( FlushModeType.COMMIT, q.getFlushMode() );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testConfigValueHandling() {
		final String name = "itemJpaQueryWithLockModeAndHints";
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();

		Query query = em.createNamedQuery( name );
		org.hibernate.query.Query hibernateQuery = (org.hibernate.query.Query) query;
		// assert the state of the query config settings based on the initial named query
		//
		//		NOTE: here we check "query options" via the Hibernate contract (allowing nullness checking); see below for access via the JPA contract
		assertNull( hibernateQuery.getQueryOptions().getFirstRow() );
		assertNull( hibernateQuery.getQueryOptions().getMaxRows() );
		assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
		assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
		assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
		// jpa timeout is in milliseconds, whereas Hibernate's is in seconds
		assertEquals( (Integer) 3, hibernateQuery.getTimeout() );

		query.setHint( QueryHints.HINT_TIMEOUT, 10 );
		em.getEntityManagerFactory().addNamedQuery( name, query );

		query = em.createNamedQuery( name );
		hibernateQuery = (org.hibernate.query.Query) query;
		// assert the state of the query config settings based on the initial named query
		//
		//		NOTE: here we check "query options" via the JPA contract
		assertEquals( 0, hibernateQuery.getFirstResult() );
		assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
		assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
		assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
		assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
		assertEquals( (Integer) 10, hibernateQuery.getTimeout() );

		query.setHint( QueryHints.SPEC_HINT_TIMEOUT, 10000 );
		em.getEntityManagerFactory().addNamedQuery( name, query );

		query = em.createNamedQuery( name );
		hibernateQuery = (org.hibernate.query.Query) query;
		// assert the state of the query config settings based on the initial named query
		assertEquals( 0, hibernateQuery.getFirstResult() );
		assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
		assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
		assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
		assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
		assertEquals( (Integer) 10, hibernateQuery.getTimeout() );

		query.setFirstResult( 51 );
		em.getEntityManagerFactory().addNamedQuery( name, query );

		query = em.createNamedQuery( name );
		hibernateQuery = (org.hibernate.query.Query) query;
		// assert the state of the query config settings based on the initial named query
		assertEquals( 51, hibernateQuery.getFirstResult() );
		assertEquals( Integer.MAX_VALUE, hibernateQuery.getMaxResults() );
		assertEquals( FlushMode.MANUAL, hibernateQuery.getHibernateFlushMode() );
		assertEquals( FlushModeType.COMMIT, hibernateQuery.getFlushMode() );
		assertEquals( CacheMode.IGNORE, hibernateQuery.getCacheMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getLockMode() );
		assertEquals( (Integer) 10, hibernateQuery.getTimeout() );

		em.getTransaction().commit();
		em.close();
	}
}
