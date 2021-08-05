//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.performance;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects data for the current process for different {@link PerformanceProfile}.
 * Additional profiles can be included and * their implementation done in {@link PerformanceProfileMonitor}
 * then passed through the method {@link DeviceMonitor#setProfiler(PerformanceProfile, PerformanceProfileMonitor)}
 */
public class DeviceMonitor {

    private static final String packageName = ApplicationProvider.getApplicationContext().getPackageName();
    private static final HashMap<PerformanceProfile, PerformanceProfileMonitor<?>> profilers = new HashMap<>();
    private static final ProcessInfo processInfo = new ProcessInfo();

    static {
        loadProcessInfo();
    }

    /**
     * Get a String representation of the name of the current device
     *
     * @return a string representation of the name of the current device
     */
    public static String getDeviceName() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        // If the device model has the name of the manufacturer, just return the model, otherwise concatenate
        // the manufacturer and the model
        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private static String capitalize(final String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Gets the current application's uid.
     *
     * @return an integer representing the application's uid
     */
    public static int getApplicationUid() {
        final Pattern pattern = Pattern.compile("userId=(\\d+)");
        final String shellCommand = "dumpsys package " + packageName;

        final Matcher matcher = pattern.matcher(AdbShellUtils.executeShellCommand(shellCommand));

        if (matcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        }
        return 0;
    }

    /**
     * Get the total System memory.
     *
     * @return a Long representing the total memory of the device in KB
     */
    public static Long getTotalMemory() {
        return processInfo.getTotalSystemMemory();
    }

    /**
     * Get the current CPU usage
     *
     * @return the current cpu usage as a percentage e.g. 3.3
     */
    public static Double getCpuUsage() {
        return getStats(PerformanceProfile.CPU, Double.class);
    }

    /**
     * Get the current memory usage.
     *
     * @return the current memory usage in KB
     */
    public static Long getMemoryUsage() {
        return getStats(PerformanceProfile.MEMORY, Long.class);
    }

    /**
     * Get the current network info.
     *
     * @return the information regarding the network usage {@link TrafficInfo}
     */
    public static TrafficInfo getNetworkTrafficInfo() {
        return getStats(PerformanceProfile.NETWORK, TrafficInfo.class);
    }

    /**
     * Get the statistics of a particular performance profile.
     *
     * @param performanceProfile the performance profile
     * @param classOfT           the class of the stats result
     * @param <T>                the type of the stats retrieved for the performance profile
     * @return the current data showing its usage.
     */
    public static <T> T getStats(@NonNull final PerformanceProfile performanceProfile, @NonNull final Class<T> classOfT) {
        final PerformanceProfileMonitor<?> monitor = profilers.get(performanceProfile);

        if (monitor == null) {
            throw new IllegalArgumentException("The performanceProfile does not have a monitor.");
        }

        loadProcessInfo();

        return classOfT.cast(monitor.getStats(processInfo));
    }

    /**
     * Fetch the current process information.
     *
     * @return the current process information {@link ProcessInfo}
     */
    public static ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Updates or adds a new profiling monitor for a particular profile.
     *
     * @param profile  the profile to update {@link PerformanceProfile}
     * @param profiler the profile monitor to use {@link PerformanceProfileMonitor}
     * @param <T>      the type of the data that will be collected by the {@link PerformanceProfileMonitor}
     */
    public static <T> void setProfiler(@NonNull final PerformanceProfile profile, @NonNull final PerformanceProfileMonitor<T> profiler) {
        profilers.put(profile, profiler);
    }

    /**
     * Returns the current profile monitor tool that's being used by the profile.
     *
     * @param profile {@link PerformanceProfile}
     * @return the profile monitor tool {@link PerformanceProfileMonitor}
     */
    public static PerformanceProfileMonitor<?> getProfiler(@NonNull final PerformanceProfile profile) {
        return profilers.get(profile);
    }

    /**
     * Fetches the current process information by executing several adb shell commands.
     */
    private static void loadProcessInfo() {
        processInfo.setUid(getApplicationUid());

        initializeCpuAndMemoryUsage();
        initializeMemoryInfo();
    }

    /**
     * Fetch the memory information by parsing output from /proc/meminfo
     * <p>
     * Example Output
     * MemTotal:        2042232 kB
     * MemFree:          329176 kB
     * MemAvailable:     800604 kB
     * Buffers:           16252 kB
     * Cached:           766452 kB
     */
    private static void initializeMemoryInfo() {
        final String shellCommand = "cat /proc/meminfo";
        final String[] commandResult = AdbShellUtils.executeShellCommand(shellCommand).split("\n");

        final long memTotal = Long.parseLong(commandResult[0].trim().split("\\s+")[1]);
        final long memFree = Long.parseLong(commandResult[1].trim().split("\\s+")[1]);
        final long memAvailable = Long.parseLong(commandResult[2].trim().split("\\s+")[1]);

        processInfo.setFreeSystemMemory(memFree);
        processInfo.setAvailableSystemMemory(memAvailable);
        processInfo.setTotalSystemMemory(memTotal);
        processInfo.setUsedSystemMemory(memTotal - memAvailable);
    }

    /**
     * Execute the top shell command to get the cpu and memory usage.
     * <p>
     * Example Output
     * Tasks: 1 total,   0 running,   1 sleeping,   0 stopped,   0 zombie
     * Mem:      1.9G total,      1.6G used,      328M free,       16M buffers
     * Swap:      1.4G total,         0 used,      1.4G free,      744M cached
     * 400%cpu   4%user   0%nice  23%sys 373%idle   0%iow   0%irq   0%sirq   0%host
     * PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
     * 1 root         20   0  31M 3.1M 1.6M S  0.0   0.1   0:05.56 init second_stage
     */
    private static void initializeCpuAndMemoryUsage() {
        final int pid = android.os.Process.myPid();

        final int numCpuCores = Runtime.getRuntime().availableProcessors();

        // Execute the top command by passing the process id.
        // -b [Batch Mode] sets the output in a parsable format.
        // -n [Exit top command after specific number of repetitions] in this case we just need it to fetch once.
        // -p [Process ID] set the process id
        final String shellCommand = "top -b -n 1 -p " + pid;
        final String[] commandResult = AdbShellUtils.executeShellCommand(shellCommand).split("\n");

        final String[] processInfoString = commandResult[5].trim().split("\\s+");

        final String cpuUsage = processInfoString[8];
        final String memoryUsage = processInfoString[9];

        processInfo.setCpuUsage(Double.parseDouble(cpuUsage) / numCpuCores);
        processInfo.setMemoryUsage(Double.parseDouble(memoryUsage));
        processInfo.setPid(pid);

    }

}
