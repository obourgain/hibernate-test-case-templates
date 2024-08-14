/*
 * Copyright 2014 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.bugs;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This template demonstrates how to develop a test case for Hibernate ORM, using its built-in unit test framework.
 * Although ORMStandaloneTestCase is perfectly acceptable as a reproducer, usage of this class is much preferred.
 * Since we nearly always include a regression test with bug fixes, providing your reproducer using this method
 * simplifies the process.
 *
 * What's even better?  Fork hibernate-orm itself, add your test case directly to a module's unit tests, then
 * submit it as a PR!
 */
public class ORMUnitTestCase extends BaseCoreFunctionalTestCase {

	// Add your entities here.
	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Shop.class,
				ShopTaxAssociation.class,
				Tracking.class,
		};
	}

	// If you use *.hbm.xml mappings, instead of annotations, add the mappings here.
	@Override
	protected String[] getMappings() {
		return new String[] {
//				"Foo.hbm.xml",
//				"Bar.hbm.xml"
		};
	}
	// If those mappings reside somewhere other than resources/org/hibernate/test, change this.
	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/test/";
	}

	// Add in any settings that are specific to your test.  See resources/hibernate.properties for the defaults.
	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );

		configuration.setProperty( AvailableSettings.SHOW_SQL, Boolean.TRUE.toString() );
		configuration.setProperty( AvailableSettings.FORMAT_SQL, Boolean.TRUE.toString() );
		//configuration.setProperty( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	public static final ReentrantLock LOCK = new ReentrantLock();

	// Add your tests, using standard JUnit.
	@Test
	public void hhh18475Test() throws Exception {
		long trackingId = 2L;
		// init the data
		// not sure about the details, it seems we need three entities
		// one case that triggers the bug is: entity1 references entity2, and entity2 has a collection of entity3 with eager fetching
			try (Session s = openSession()) {
			Transaction tx = s.beginTransaction();
			Shop shop = new Shop();
			shop.setId(1L);
			ShopTaxAssociation shopTaxAssociation = new ShopTaxAssociation(shop, UUID.randomUUID());
			shop.getShopTaxAssociations().add(shopTaxAssociation);
			session.save(shop);

			Tracking offerImportTracking = new Tracking();
			offerImportTracking.setId(trackingId);
			offerImportTracking.setShop(shop);
			session.save(offerImportTracking);

			tx.commit();
		}

		ExecutorService executorService = Executors.newFixedThreadPool(2);

		LOCK.lock();
		try {
			executorService.submit(() -> {
				try (Session session = openSession()) {
					Tracking tracking = session.load(Tracking.class, trackingId);
					tracking.getShop();
				}
			});
			// wait for the thread to be stuck at the right point
			// if reproducing with a debugger, remove this loop
			while (!LOCK.hasQueuedThreads()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}

			// run another Callable to trigger the issue
			Future<?> future = executorService.submit(() -> {
				try (Session session2 = openSession()) {
					Tracking tracking = session2.load(Tracking.class, trackingId);
					tracking.getShop();
				}
			});

			try {
				future.get();
				Assert.fail("future should contain an exception");
			} catch (Exception e) {
				// using getCause() to unwrap the ExecutionException
				Assert.assertEquals(e.getCause().getClass(), NullPointerException.class);
				StringWriter out = new StringWriter();
				try (PrintWriter printWriter = new PrintWriter(out)) {
					e.getCause().printStackTrace(printWriter);
				}
				Assert.assertTrue(out.toString().startsWith("java.lang.NullPointerException: Cannot invoke \"org.hibernate.loader.plan.exec.process.spi.EntityReferenceInitializer.getEntityReference()\" because \"entityReferenceInitializer\" is null\n" +
															"\tat org.hibernate.loader.plan.exec.process.internal.AbstractRowReader.resolveEntityKey(AbstractRowReader.java:105)\n" +
															"\tat org.hibernate.loader.plan.exec.process.internal.AbstractRowReader.resolveEntityKey(AbstractRowReader.java:121)\n" +
															"\tat org.hibernate.loader.plan.exec.process.internal.AbstractRowReader.resolveEntityKey(AbstractRowReader.java:109)\n" +
															"\tat org.hibernate.loader.plan.exec.process.internal.AbstractRowReader.readRow(AbstractRowReader.java:72)"));
			}
		} finally {
			LOCK.unlock();
		}
	}
}
