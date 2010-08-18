/*
 *    Copyright 2010 The MyBatis Team
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.caches.ehcache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.apache.ibatis.cache.Cache;
import org.junit.Test;

/**
 * 
 *
 * @version $Id$
 */
public final class EhcacheTestCase {

    private static final String DEFAULT_ID = "EHCACHE";

    private static Cache newCache() {
        return new EhcacheCache(DEFAULT_ID);
    }

    @Test
    public void shouldDemonstrateHowAllObjectsAreKept() {
        Cache cache = newCache();
        for (int i = 0; i < 100000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
        assertEquals(100000, cache.getSize());
    }

    @Test
    public void shouldDemonstrateCopiesAreEqual() {
        Cache cache = newCache();
        for (int i = 0; i < 1000; i++) {
            cache.putObject(i, i);
            assertEquals(i, cache.getObject(i));
        }
    }

    @Test
    public void shouldRemoveItemOnDemand() {
        Cache cache = newCache();
        cache.putObject(0, 0);
        assertNotNull(cache.getObject(0));
        cache.removeObject(0);
        assertNull(cache.getObject(0));
    }

    @Test
    public void shouldFlushAllItemsOnDemand() {
        Cache cache = newCache();
        for (int i = 0; i < 5; i++) {
            cache.putObject(i, i);
        }
        assertNotNull(cache.getObject(0));
        assertNotNull(cache.getObject(4));
        cache.clear();
        assertNull(cache.getObject(0));
        assertNull(cache.getObject(4));
    }

}
