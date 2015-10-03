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

package io.trivium.test;

import io.trivium.Registry;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Tester {
    public static Logger logger = Logger.getLogger(Tester.class.getName());

    public static void success(String... args){
        if(args!=null && args.length>1){
            logger.log(Level.INFO,"success " + args[0], args);
        }else {
            logger.log(Level.INFO,"success", args);
        }
    }

    public static void error(String... args){
        if(args!=null && args.length>1){
            logger.log(Level.INFO,"error "+args[0],args);
        }else {
            logger.log(Level.INFO,"error", args);
        }
    }

    /**
     * runs all registered test cases
     * @return whether an error happened during testing
     */
    public static boolean runAll(){
        logger.info("running test suite");
        logger.log(Level.INFO,"running test suite 2");
        boolean errorHappened = false;
        Iterator<TestCase> iter = Registry.INSTANCE.testcases.values().iterator();
        while(iter.hasNext()){
            TestCase tc = iter.next();
            logger.log(Level.INFO,"test {0}: {1} {2}",new String[]{tc.getTypeId().toString(),tc.getClassName(),tc.getMethodName()});
            try {
                tc.run();
                logger.log(Level.INFO,"test {0}: succeeded", tc.getTypeId().toString());
            }catch(Exception ex){
                logger.log(Level.SEVERE,"test "+tc.getTypeId().toString()+": failed with exception", ex);
                errorHappened=true;
            }
        }
        return errorHappened;
    }
}
