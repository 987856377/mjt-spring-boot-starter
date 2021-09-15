package com.springboot.mjt.task;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.task
 * @Author xuzhenkui
 * @Date 2021/9/15 14:20
 */
public class XMLResourceRecursiveTask extends RecursiveTask<Map<String, String>> {
    private static final int THRESHOLD = 100;
    private final Resource[] resources;

    public XMLResourceRecursiveTask(Resource[] resources) {
        this.resources = resources;
    }

    /**
     * The main computation performed by this task.
     *
     * @return the result of the computation
     */
    @Override
    protected Map<String, String> compute() {
        if (resources.length > THRESHOLD) {
            Map<String, String> resourceMap = new ConcurrentHashMap<>();

            ForkJoinTask.invokeAll(createSubTasks())
                    .stream()
                    .map(ForkJoinTask::join)
                    .forEach(resourceMap::putAll);

            return resourceMap;
        } else {
            return processing(resources);
        }
    }

    private Collection<XMLResourceRecursiveTask> createSubTasks() {
        List<XMLResourceRecursiveTask> dividedTasks = new ArrayList<>();
        dividedTasks.add(new XMLResourceRecursiveTask(Arrays.copyOfRange(resources, 0, resources.length / 2)));
        dividedTasks.add(new XMLResourceRecursiveTask(Arrays.copyOfRange(resources, resources.length / 2, resources.length)));
        return dividedTasks;
    }

    private Map<String, String> processing(Resource[] resources) {
        Map<String, String> map = new ConcurrentHashMap<>();

        Arrays.stream(resources).forEach(resource -> {
            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(resource.getInputStream());
                Element namespace = document.getRootElement();

                String mapper = namespace.attributeValue("mapper");
                List<Element> sqlList = namespace.elements("sql");

                sqlList.forEach(sql -> map.put(mapper + "." + sql.attributeValue("id"), sql.getTextTrim()));

            } catch (DocumentException | IOException e) {
                e.printStackTrace();
            }
        });
        return map;
    }

}
