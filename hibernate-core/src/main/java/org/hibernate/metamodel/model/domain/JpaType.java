/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

/**
 * Base contract for Hibernate's extension of the JPA type system.
 *
 * @apiNote The "real" JPA type system is more akin to
 * {@link SimpleType}.  We begin our JPA type system extension
 * a "level above" that.  This is to allow for:
 * 		1) JPA does not define a Type for collections.  It's
 * 			understandable why, but leads to limitations in
 * 			regards to being able to understand the type of an
 * 			attribute - in JPA, when the attribute is plural the
 * 			only descriptor info available is for the the collection
 * 			is its Java type (Class).
 * 		2) specialized types like ANY
 *
 *
 * @param <J> The Java type for this JPA Type
 *
 * @author Steve Ebersole
 */
public interface JpaType<J> extends javax.persistence.metamodel.Type<J> {

}
