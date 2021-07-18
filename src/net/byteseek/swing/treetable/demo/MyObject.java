package net.byteseek.swing.treetable.demo;

import java.util.ArrayList;
import java.util.List;

public class MyObject {

    private String description;
    private long   size;
    private boolean enabled;
    private List<MyObject> children;

    public MyObject(String description, long size, boolean enabled) {
        this.description = description;
        this.size = size;
        this.enabled = enabled;
        children = new ArrayList<>();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void addChild(MyObject test) {
        children.add(test);
    }

    public void addChildren(List<MyObject> children) {
        this.children.addAll(children);
    }

    public List<MyObject> getChildren() {
        return children;
    }
}
