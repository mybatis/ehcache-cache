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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.cache.CacheException;

/**
 * Cache adapter for Ehcache.
 *
 * @version $Id$
 */
public final class EhcacheCache implements Cache {

    /**
     * The cache manager reference.
     */
    private static final CacheManager CACHE_MANAGER = createCacheManager();

    /**
     * Looks for "/ehcache.xml" classpath resource and builds the relative
     * {@code CacheManager}; if it's no found or it is impossible to load it,
     * returns the default manager.
     *
     * @return the application cache manager.
     */
    private static CacheManager createCacheManager() {
        CacheManager cacheManager;
        InputStream input = EhcacheCache.class.getResourceAsStream("/ehcache.xml");

        if (input != null) {
            try {
                cacheManager = CacheManager.create(input);
            } catch (Throwable t) {
                cacheManager = CacheManager.create();
            } finally {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        } else {
            cacheManager = CacheManager.create();
        }

        return cacheManager;
    }

    /**
     * The {@code ReadWriteLock}.
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * The cache id.
     */
    private final String id;

    /**
     *
     *
     * @param id
     */
    public EhcacheCache(final String id) {
        if (id == null) {
            throw new IllegalArgumentException("Cache instances require an ID");
        }
        this.id = id;
        if (!CACHE_MANAGER.cacheExists(this.id)) {
            CACHE_MANAGER.addCache(this.id);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        this.getCache().removeAll();
    }

    /**
     * {@inheritDoc}
     */
    public String getId() {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    public Object getObject(Object key) {
        try {
            Element cachedElement = this.getCache().get(key.hashCode());
            if (cachedElement == null) {
                return null;
            }
            return cachedElement.getObjectValue();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public ReadWriteLock getReadWriteLock() {
        return this.readWriteLock;
    }

    /**
     * {@inheritDoc}
     */
    public int getSize() {
        try {
            return this.getCache().getSize();
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void putObject(Object key, Object value) {
        try {
            this.getCache().put(new Element(key.hashCode(), value));
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object removeObject(Object key) {
        try {
            Object obj = this.getObject(key);
            this.getCache().remove(key.hashCode());
            return obj;
        } catch (Throwable t) {
            throw new CacheException(t);
        }
    }

    /**
     * Returns the ehcache manager for this cache.
     *
     * @return the ehcache manager for this cache.
     */
    private Ehcache getCache() {
        return CACHE_MANAGER.getCache(this.id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Cache)) {
            return false;
        }

        Cache otherCache = (Cache) obj;
        return this.id.equals(otherCache.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "EHCache {"
                + this.id
                + "}";
    }

}
