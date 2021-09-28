package com.panl.cache;

/**
 * @author panlei
 * @date 2021/9/28 下午1:50
 */
public interface Cache {
    Object get(Object key);
    Object put(Object key, Object value);

    /**
     *
     * @param expireTime 自定义缓存过期时间，单位毫秒
     * @return
     */
    Object put(Object key, Object value,long expireTime);
    boolean containsKey(Object key);
    void clear();
}
