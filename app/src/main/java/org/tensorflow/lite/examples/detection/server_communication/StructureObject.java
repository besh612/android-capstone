package org.tensorflow.lite.examples.detection.server_communication;

public class StructureObject {

    private int id;
    private String name;

    @Override
    public String toString() {
        return id + "-" + name;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
