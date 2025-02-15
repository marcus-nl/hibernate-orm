/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.criteria;

import java.util.List;
import jakarta.persistence.criteria.AbstractQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import jakarta.persistence.metamodel.EntityType;

/**
 * Commonality between a JPA {@link JpaCriteriaQuery} and {@link JpaSubQuery},
 * mainly in the form of delegation to {@link JpaQueryStructure}
 *
 * @author Steve Ebersole
 */
public interface JpaSelectCriteria<T> extends AbstractQuery<T>, JpaCriteriaBase {
	/**
	 * The query structure.  See {@link JpaQueryStructure} for details
	 */
	JpaQueryStructure<T> getQuerySpec();
	/**
	 * The query structure.  See {@link JpaQueryStructure} for details
	 */
	JpaQueryPart<T> getQueryPart();

	/**
	 * Create and add a query root corresponding to the given sub query,
	 * forming a cartesian product with any existing roots.
	 *
	 * @param subquery  the sub query
	 * @return query root corresponding to the given sub query
	 */
	<X> JpaDerivedRoot<X> from(Subquery<X> subquery);

	/**
	 * Create and add a query root corresponding to the given lateral sub query,
	 * forming a cartesian product with any existing roots.
	 *
	 * @param subquery  the sub query
	 * @return query root corresponding to the given sub query
	 */
	<X> JpaDerivedRoot<X> fromLateral(Subquery<X> subquery);

	/**
	 * Create and add a query root corresponding to the given sub query,
	 * forming a cartesian product with any existing roots.
	 * If the sub query is marked as lateral, it may access previous from elements.
	 *
	 * @param subquery  the sub query
	 * @param lateral whether to allow access to previous from elements in the sub query
	 * @return query root corresponding to the given sub query
	 */
	<X> JpaDerivedRoot<X> from(Subquery<X> subquery, boolean lateral);

	@Override
	JpaSelectCriteria<T> distinct(boolean distinct);

	@Override
	JpaSelection<T> getSelection();

	@Override
	<X> JpaRoot<X> from(Class<X> entityClass);

	@Override
	<X> JpaRoot<X> from(EntityType<X> entity);

	@Override
	JpaPredicate getRestriction();

	@Override
	JpaSelectCriteria<T> where(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> where(Predicate... restrictions);

	@Override
	JpaSelectCriteria<T> groupBy(Expression<?>... grouping);

	@Override
	JpaSelectCriteria<T> groupBy(List<Expression<?>> grouping);

	@Override
	JpaPredicate getGroupRestriction();

	@Override
	JpaSelectCriteria<T> having(Expression<Boolean> restriction);

	@Override
	JpaSelectCriteria<T> having(Predicate... restrictions);
}
