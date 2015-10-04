/*
 * Copyright 2015 Jens Walter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.trivium;

import io.trivium.anystore.AnyClient;
import io.trivium.anystore.ObjectRef;
import io.trivium.dep.org.apache.commons.io.IOUtils;
import io.trivium.extension._f70b024ca63f4b6b80427238bfff101f.TriviumObject;
import io.trivium.extension.binding.Binding;
import io.trivium.extension.task.InputType;
import io.trivium.extension.type.Type;
import io.trivium.extension.task.Task;
import io.trivium.test.TestCase;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public enum Registry {
    INSTANCE;

    Logger log = Logger.getLogger(getClass().getName());

    public ConcurrentHashMap<ObjectRef, Class<? extends Task>> tasks = new ConcurrentHashMap<>();
    public ConcurrentHashMap<ObjectRef, ArrayList<Task>> taskSubscription = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ObjectRef, Class<? extends Type>> types = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ObjectRef, Class<? extends Binding>> bindings = new ConcurrentHashMap<>();
    public ConcurrentHashMap<ObjectRef, Binding> bindingInstances = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ObjectRef, TestCase> testcases = new ConcurrentHashMap<>();

    public void reload() {
        final String PREFIX = "META-INF/services/";
        ClassLoader tvmLoader = ClassLoader.getSystemClassLoader();
        //types
        try {
            Enumeration<URL> resUrl = tvmLoader.getResources(PREFIX + "io.trivium.extension.type.Type");
            while (resUrl.hasMoreElements()) {
                URL url = resUrl.nextElement();
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                List<String> lines = IOUtils.readLines(is, "UTF-8");
                is.close();
                for (String line : lines) {
                    Class<? extends Type> clazz = (Class<? extends Type>) Class.forName(line);
                    Type prototype = clazz.newInstance();
                    if (!types.containsKey(prototype.getTypeId())) {
                        types.put(prototype.getTypeId(), clazz);
                    }
                    log.log(Level.FINE, "registered type '{0}'", prototype.getTypeName());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "dynamically loading types failed", ex);
        }

        //bindings
        try {
            Enumeration<URL> resUrl = tvmLoader.getResources(PREFIX + "io.trivium.extension.binding.Binding");
            while (resUrl.hasMoreElements()) {
                URL url = resUrl.nextElement();
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                List<String> lines = IOUtils.readLines(is, "UTF-8");
                is.close();
                for (String line : lines) {
                    Class<? extends Binding> clazz = (Class<? extends Binding>) Class.forName(line);
                    Binding prototype = clazz.newInstance();
                    if (!bindings.containsKey(prototype.getTypeId())) {
                        bindings.put(prototype.getTypeId(), clazz);
                        //register prototype
                        bindingInstances.put(prototype.getTypeId(),prototype);
                    }
                    log.log(Level.FINE, "registered binding '{0}'", prototype.getName());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "dynamically loading bindings failed", ex);
        }

        //tasks
        try {
            Enumeration<URL> resUrl = tvmLoader.getResources(PREFIX + "io.trivium.extension.task.Task");
            while (resUrl.hasMoreElements()) {
                URL url = resUrl.nextElement();
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                List<String> lines = IOUtils.readLines(is, "UTF-8");
                is.close();
                for (String line : lines) {
                    Class<? extends Task> clazz = (Class<? extends Task>) Class.forName(line);
                    Task prototype = clazz.newInstance();
                    if (!tasks.containsKey(prototype.getTypeId())) {
                        tasks.put(prototype.getTypeId(), clazz);

                        //register subscription
                        ArrayList<InputType> inputTypes = prototype.getInputTypes();
                        for (InputType ref : inputTypes) {
                            ArrayList<Task> a = taskSubscription.get(ref);
                            if (a == null) {
                                ArrayList<Task> all = new ArrayList<>();
                                all.add(prototype);
                                taskSubscription.put(ref.typeId, all);
                            } else {
                                if (!a.contains(prototype)) {
                                    a.add(prototype);
                                }
                            }
                        }
                    }
                    log.log(Level.FINE, "registered binding '{0}'", prototype.getName());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "dynamically loading bindings failed", ex);
        }

        //testcases
//        testcaseLoader.reload();
//        Iterator<TestCase> testIter = testcaseLoader.iterator();
//        while (testIter.hasNext()) {
//            TestCase testcase = testIter.next();
//            if (!testcases.containsKey(testcase.getTypeId())) {
//                testcases.put(testcase.getTypeId(), testcase);
//            }
//        }

        //testcases
        try {
            Enumeration<URL> resUrl = tvmLoader.getResources(PREFIX + "io.trivium.test.TestCase");
            while (resUrl.hasMoreElements()) {
                URL url = resUrl.nextElement();
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream is = connection.getInputStream();
                List<String> lines = IOUtils.readLines(is, "UTF-8");
                is.close();
                for (String line : lines) {
                    Class<? extends TestCase> clazz = (Class<? extends TestCase>) Class.forName(line);
                    TestCase prototype = clazz.newInstance();
                    if (!testcases.containsKey(prototype.getTypeId())) {
                        testcases.put(prototype.getTypeId(), prototype);
                    }
                    log.log(Level.FINE, "registered testcase '{0}'", prototype.getTypeId());
                }
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, "dynamically loading testcases failed", ex);
        }
    }

    public void notify(TriviumObject tvm) {
        ObjectRef ref = tvm.getTypeId();
        ArrayList<Task> prototypes = taskSubscription.get(ref);
        //calculate activity
        if (prototypes != null) {
            for (Task prototype : prototypes) {
                if (prototype.isApplicable(tvm)) {
                    try {
                        Task task = tasks.get(prototype.getTypeId()).newInstance();
                        task.populateInput(tvm);
                        task.eval();
                        ArrayList<TriviumObject> output = task.extractOutput();
                        for (TriviumObject o : output) {
                            AnyClient.INSTANCE.storeObject(o);
                        }
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, "error while running task '{0}'", prototype.getName());
                        log.log(Level.SEVERE, "got exception", ex);
                    }
                }
            }
        }
    }
}
