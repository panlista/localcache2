package com.panl.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 简单缓存实现
 * @author panl
 * @date 2021/4/22 13:33
 */
public class SimpleCache implements Cache {

    public static void main(String[] args) {
        Cache cache = SimpleCache.getCache();
        cache.put("1","2");
        System.out.println(cache.get("1"));
    }

    private static volatile SimpleCache simpleCache;
    //缓存实体
    private final Map<Object, Node> map = new ConcurrentHashMap<Object, Node>();
    //缓存失效时间 默认1小时 可以自定义（单位毫秒）
    private long expireTime = 60*60*1000;
    //定时清除过期缓存间隔（秒）
    private int invalidTimer = 60;
    //缓存最大容量
    private long maxSize = 1024;
    //节点总数
    private int count = 0;
    //头节点和尾节点
    private Node head, tail;

    /**
     * 创建对象时初始化清除过期缓存定时任务,定义双向链表的头和尾节点
     */
    private SimpleCache(){
//        new Thread(new TimeoutTimerThread()).start();
        this.timeoutScheduledHandler();
        head = new Node();
        head.pre = null;

        tail = new Node();
        tail.next = null;
        //头节点的下一个为尾节点
        head.next = tail;
        //尾节点的上一个尾头节点
        tail.pre = head;
    }


    /**
     * 获取单例方法
     * @return
     */
    public static Cache getCache(){
        if(simpleCache == null){//1
            synchronized (SimpleCache.class){//2
                if(simpleCache == null){//3
                    simpleCache = new SimpleCache();//4
                }
            }
        }
        return simpleCache;
    }

    /**
     * 获取
     * @param key
     * @return
     */
    public Object get(Object key){
        if(!containsKey(key)){
            return null;
        }
        Node node = map.get(key);
        if(node.expireTime > (getNow() - node.writeTime)){

        }
        this.moveNode2Head(node);
        return node.value;
    }

    /**
     * 插入
     * @param key
     * @param value
     * @return
     */
    public Object put(Object key, Object value){
        return put(key,value,expireTime);
    }

    /**
     * 判断key是否存在
     * @param key
     * @return
     */
    public boolean containsKey(Object key) {
        checkKey(key);
        return map.containsKey(key);
    }

    /**
     * 清空缓存
     */
    public void clear() {
        head.next = tail;
        tail.pre = head;
        map.clear();
    }

    /**
     * 插入
     * @param key
     * @param value
     * @param expireTime 自定义过期时间
     * @return
     */
    public Object put(Object key, Object value, long expireTime){
        checkParam(key, value);
        Node node = map.get(key);
        if(node == null){
            node = new Node();
            node.key = key;
            node.value = value;
            node.writeTime = getNow();
            node.expireTime = expireTime;
            this.addNode(node);
            count++;
            //检查缓存是否已达到最大容量
            if(count > maxSize){
                //删掉最后一个
                Node tail = this.removeTail();
                this.removeNode(tail);
                count--;
            }
        }else{
            node.value = value;
            node.writeTime = getNow();
            node.expireTime = expireTime;
            //把此节点移到头部
            this.moveNode2Head(node);
        }
        map.put(key,node);
        return value;
    }

    /**
     * 把节点移动到头部
     * 先把链表中该节点删掉，重新在头部插入一个
     * @param node
     */
    final void moveNode2Head(Node node){
        this.removeNode(node);
        this.addNode(node);
    }

    /**
     * 删除节点
     * 通过改变节点的指针删除
     * @param node 要删除的节点
     */
    final void removeNode(Node node){
        Node pre = node.pre;
        Node next = node.next;

        pre.next = next;
        next.pre = pre;
    }

    /**
     * 添加节点
     * 始终在head后面添加新节点
     * @param node
     */
    final void addNode(Node node){
        node.pre = head;
        node.next = head.next;

        head.next.pre =node;
        head.next = node;
    }

    /**
     * 删除尾部节点
     * @return
     */
    final Node removeTail(){
        Node res = tail.pre;
        this.removeNode(res);
        return res;
    }
    final long getNow(){
        return System.currentTimeMillis();
    }

    final void checkParam(Object key, Object value){
        if(key == null || value == null)
            throw new IllegalArgumentException();
    }

    final void checkKey(Object key){
        if(key == null)
            throw new IllegalArgumentException();
    }

    /**
     * 超时缓存清理程序
     */
    final void timeoutScheduledHandler(){
        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            this.expireCache();
        }, invalidTimer, invalidTimer, TimeUnit.SECONDS);
    }

    /**
     * 清理过期缓存
     * @throws Exception
     */
    private void expireCache(){
        for (Object key : map.keySet()) {
            Node node = map.get(key);
            //缓存已存活时间
            long timoutTime = (getNow() - node.writeTime);
            if (node.expireTime > timoutTime) {
                continue;
            }
            //清除过期缓存
            removeNode(node);
            map.remove(key);
        }
        System.out.println(map.size());
    }

    /**
     * 清理过期缓存线程
     */
//    final class TimeoutTimerThread implements Runnable {
//        public void run() {
//            while (true) {
//                try {
//                    //设置定时任务周期
//                    TimeUnit.SECONDS.sleep(invalidTimer);
//                    expireCache();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//
//    }

    final class Node{
        //下个节点
        private Node next;
        //上个节点
        private Node pre;
        //键
        private Object key;
        // 值
        private Object value;
        // 写入时间
        private long writeTime;
        //失效时间（毫秒）
        private long expireTime;

        Node(){
        }
    }

}
