package com.emos.platform.jobs;

public interface JobHandler {
    String type();

    void handle(Job job);
}
