package com.guc.activemqtest.datastructure;

import java.util.List;

public class LinkQueue<T> {
    private Node<T> mHeader;
    private Node<T> mFooter;
    private int mSize;

    public LinkQueue() {
        mFooter = mHeader = null;
    }

    public void enQueue(T data) {
        Node<T> node = new Node<>(data);
        if (isEmpty()) {
            mHeader = mFooter = node;
        } else {
            mFooter.setNext(node);
            mFooter = node;
        }
        mSize++;
    }

    public void enQueue(List<T> data) {
        for (T t : data) {
            enQueue(t);
        }
    }

    public T deQueue() {
        if (isEmpty()) {
            return null;
        }
        Node<T> delete = mHeader;
        mHeader = delete.getNext();
        delete.setNext(null);
        mSize--;

        if (mSize == 0) {
            // 删除掉最后一个元素时，front值已经为null，但rear还是指向该节点，需要将rear置为null
            // 最后一个结点front和rear两个引用都没指向它，帮助GC处理该节点对象
            mFooter = mHeader;
        }

        return delete.getData();
    }

    public boolean isEmpty() {
        return mHeader == null && mFooter == null;
    }

    public int size() {
        return this.mSize;
    }

    private static class Node<T> {
        // 存储的数据
        private T mData;
        // 下一个节点的引用
        private Node<T> mNext;

        private Node(T data) {
            this.mData = data;
        }

        private T getData() {
            return mData;
        }

        private Node<T> getNext() {
            return mNext;
        }

        private void setNext(Node<T> next) {
            this.mNext = next;
        }
    }
}
