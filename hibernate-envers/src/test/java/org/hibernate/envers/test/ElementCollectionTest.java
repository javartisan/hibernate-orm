/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Tuple;

import org.hibernate.envers.Audited;
import org.hibernate.envers.strategy.ValidityAuditStrategy;
import org.hibernate.envers.test.tools.TablePrinter;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-12607")
public class ElementCollectionTest extends BaseEnversJPAFunctionalTestCase {

	@Entity(name = "TestEntity")
	@Audited
	public static class TestEntity {
		@Id
		private Integer id;

		@ElementCollection
		private Map<String, Emb> embs1;

		public TestEntity() {

		}

		public TestEntity(Integer id) {
			this.id = id;
		}

		public Map<String, Emb> getEmbs1() {
			return embs1;
		}

		public void setEmbs1(Map<String, Emb> embs1) {
			this.embs1 = embs1;
		}
	}

	@Embeddable
	public static class Emb implements Serializable {
		private String value;

		public Emb() {

		}

		public Emb(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { TestEntity.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		System.out.println( "************************************************************ " );
		System.out.println( "REV1 - Insert 2 rows into collection" );
		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = new TestEntity( 1 );
			e.setEmbs1( new HashMap<>() );
			e.getEmbs1().put( "a", new Emb( "value1" ) );
			e.getEmbs1().put( "b", new Emb( "value2" ) );
			entityManager.persist( e );
		} );

		TablePrinter.print( this::entityManagerFactory, "TestEntity_AUD", "TestEntity_embs1_AUD" );

		System.out.println( "************************************************************ " );
		System.out.println( "REV2 - Replace A with value1 as A with value 3 in collection" );
		doInJPA( this::entityManagerFactory, entityManager -> {
			TestEntity e = entityManager.find( TestEntity.class, 1 );
			e.getEmbs1().put( "a", new Emb( "value3" ) );
		} );

		// ValidityAuditStrategy
		// always 4 values when equals/hashCode is implemented
		// +-----+---------+---------------+-----------+--------+--------+
		// | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | REVEND | VALUE  |
		// +-----+---------+---------------+-----------+--------+--------+
		// | 1   | 0       | 1             | a         | 2      | value1 |
		// | 1   | 0       | 1             | b         | null   | value2 |
		// | 2   | 0       | 1             | a         | null   | value3 |
		// | 2   | 2       | 1             | a         | null   | value1 |
		// +-----+---------+---------------+-----------+--------+--------+

		// DefaultAuditStrategy
		// always 4 values when equals/hashCode is implemented
		// +-----+---------+---------------+-----------+--------+
		// | REV | REVTYPE | TESTENTITY_ID | EMBS1_KEY | VALUE  |
		// +-----+---------+---------------+-----------+--------+
		// | 1   | 0       | 1             | a         | value1 |
		// | 1   | 0       | 1             | b         | value2 |
		// | 2   | 0       | 1             | a         | value3 |
		// | 2   | 2       | 1             | a         | value1 |
		// +-----+---------+---------------+-----------+--------+

		TablePrinter.print( this::entityManagerFactory, "TestEntity_AUD", "TestEntity_embs1_AUD" );

		if ( ValidityAuditStrategy.class.getName().equals( getAuditStrategy() ) ) {
			System.out.println( "************************************************************ " );
			doInJPA( this::entityManagerFactory, entityManager -> {
				List<Tuple> results = entityManager
						.createNativeQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD WHERE REVEND IS NULL", Tuple.class )
						.getResultList();
				assertEquals( 1, results.size() );
				assertEquals( BigInteger.valueOf( 3 ), results.get( 0 ).get( 0 ) );
			} );
			doInJPA( this::entityManagerFactory, entityManager -> {
				List<Tuple> results = entityManager
						.createNativeQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD", Tuple.class )
						.getResultList();
				assertEquals( 1, results.size() );
				assertEquals( BigInteger.valueOf( 4 ), results.get( 0 ).get( 0 ) );
			} );
		}
		else {
			System.out.println( "************************************************************ " );
			doInJPA( this::entityManagerFactory, entityManager -> {
				List<Tuple> results = entityManager
						.createNativeQuery( "SELECT COUNT(1) FROM TestEntity_embs1_AUD", Tuple.class )
						.getResultList();
				assertEquals( 1, results.size() );

				assertEquals( BigInteger.valueOf( 4 ), results.get( 0 ).get( 0 ) );
			} );
		}
	}

	@Test
	public void testRevisionHistory1() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 1 );
		assertEquals( 2, e.getEmbs1().size() );
		assertEquals( "value1", e.getEmbs1().get( "a" ).getValue() );
		assertEquals( "value2", e.getEmbs1().get( "b" ).getValue() );
	}

	@Test
	public void testRevisionHistory2() {
		TestEntity e = getAuditReader().find( TestEntity.class, 1, 2 );
		assertEquals( 2, e.getEmbs1().size() );
		assertEquals( "value3", e.getEmbs1().get( "a" ).getValue() );
		assertEquals( "value2", e.getEmbs1().get( "b" ).getValue() );
	}
}
