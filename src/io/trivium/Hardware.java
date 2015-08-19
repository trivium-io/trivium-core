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

import io.trivium.extension._9ff9aa69ff6f4ca1a0cf0e12758e7b1e.WeightedAverage;
import io.trivium.profile.DataPoints;
import io.trivium.profile.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.MBeanServerConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Timer;
import java.util.TimerTask;

public class Hardware {

    /**
     * core count
     */
    public static int cpuCount = 1;

    /**
     * size in mb
     */
    public static long memSize = 1024;

    /**
     * filesystem the anystore resides on
     */
    public static String fsType = "unknown";

    public static void discover() throws Exception {
        // detect os
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            // mac
            discoverMac();
        } else if (os.contains("win")) {
            // windows
            throw new Exception("discovery for this operating system is not supported");
        } else if (os.contains("nux")) {
            // linux
            discoverLinux();
        }
        Profiler.INSTANCE.initAverage(new WeightedAverage(DataPoints.OS_CPU_USAGE));
        MBeanServerConnection mbsc = ManagementFactory.getPlatformMBeanServer();
        final OperatingSystemMXBean osMBean = ManagementFactory.newPlatformMXBeanProxy(mbsc,ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME,OperatingSystemMXBean.class);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                Profiler.INSTANCE.avg(DataPoints.OS_CPU_USAGE,osMBean.getSystemLoadAverage());
            }
        },1,5000);
    }

    public static String runInOS(String[] cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            InputStreamReader isr = new InputStreamReader(p.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String line;
            StringBuilder result = new StringBuilder();
            while ((line = br.readLine()) != null) {
                result.append(line);
            }
            br.close();
            isr.close();
            return result.toString().trim();
        } catch (Exception e) {
            Logger log = LogManager.getLogger(Hardware.class);
            log.error("error exists with the current command", e);
        }
        return "";
    }

    private static void discoverMac() {
        // get cpu count
        cpuCount = Runtime.getRuntime().availableProcessors();

        // get memory size
        String size = runInOS(new String[] { "/bin/sh", "-c", "sysctl -n hw.memsize" });
        try {
            long i = Long.parseLong(size);
            if (i > 0)
                memSize = i;
        } catch (Exception e) {
            Logger log = LogManager.getLogger(Hardware.class);
            log.error(e);
        }
    }

    private static void discoverLinux() {
        // get cpu count
        cpuCount = Runtime.getRuntime().availableProcessors();

        // get memory size
        String size = runInOS(new String[] { "/bin/sh", "-c", "/usr/bin/awk '/MemTotal:/ { print $2 }' /proc/meminfo" });
        Logger log = LogManager.getLogger(Hardware.class);
        
        try {
            try {
                long i = Long.parseLong(size);
                if (i > 0)
                    memSize = i * 1024L;
            } catch (Exception e) {
                log.error("cannot read memory size", e);
            }
            String basePath = Central.getProperty("basePath");
            // checking filesystem
            String cmd = "/bin/df -T '" + basePath + "' | /usr/bin/awk '{print $2}' | tail -n1";
            fsType = runInOS(new String[] { "/bin/sh", "-c", cmd });

            Central.setProperty("fsType", fsType);
            log.info("system is running on linux.");
            log.info("cpu count is {}", cpuCount);
            log.info("memory size is {}", memSize);
            log.info("anystore path is {}", basePath);
            log.info("anystore filesystem type is {}", fsType);
        } catch (Exception e) {
            log.error("os discovery failed", e);
        }
    }
}
