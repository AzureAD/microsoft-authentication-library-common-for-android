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

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Collects data for the current process for different {@link PerformanceProfile}. Additional profiles can included and
 * their implementation done in {@link PerformanceProfileMonitor} then passed through the method {@link DeviceMonitor#setProfiler(PerformanceProfile, PerformanceProfileMonitor)}
 */
public class DeviceMonitor {

    private static final String packageName = ApplicationProvider.getApplicationContext().getPackageName();
    private static final HashMap<PerformanceProfile, PerformanceProfileMonitor<?>> profilers = new HashMap<>();
    private static ProcessInfo processInfo = null;

    static {
        loadProcessInfo();
        setProfiler(PerformanceProfile.CPU, new CPUMonitor());
        setProfiler(PerformanceProfile.MEMORY, new MemoryMonitor());
        setProfiler(PerformanceProfile.NETWORK, new NetworkUsageMonitor());
    }

    /**
     * Gets the current application's uid
     *
     * @return the application uid
     */
    public static int getApplicationUid() {
        final Pattern pattern = Pattern.compile("userId=(\\d+)");
        final String command = "dumpsys package " + packageName;

        final Matcher matcher = pattern.matcher(AdbShellUtils.executeShellCommand(command));

        if (matcher.find()) {
            return Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
        }
        return 0;
    }

    /**
     * Get the current CPU usage
     *
     * @return the current cpu usage as a percentage e.g. 3.3%
     */
    public static String getCpuUsage() {
        return getStats(PerformanceProfile.CPU, String.class);
    }

    /**
     * Get the current memory usage
     *
     * @return the current memory usage in KB
     */
    public static Long getMemoryUsage() {
        return getStats(PerformanceProfile.MEMORY, Long.class);
    }

    /**
     * Get the current network info
     *
     * @return the information regarding the network usage
     */
    public static NetworkUsageMonitor.TrafficInfo getNetworkTrafficInfo() {
        return getStats(PerformanceProfile.NETWORK, NetworkUsageMonitor.TrafficInfo.class);
    }

    /**
     * Get the statistics of a particular performance profile
     *
     * @param performanceProfile the performance profile
     * @param classOfT           the class of the stats result
     * @return the current data showing its usage.
     */
    public static <T> T getStats(PerformanceProfile performanceProfile, Class<T> classOfT) {
        PerformanceProfileMonitor<?> monitor = profilers.get(performanceProfile);

        if (monitor == null) {
            throw new IllegalArgumentException("The performanceProfile does not have a monitor.");
        }

        return classOfT.cast(monitor.getStats(processInfo));
    }

    /**
     * Fetch the current process information
     *
     * @return the current process information
     */
    public static ProcessInfo getProcessInfo() {
        return processInfo;
    }

    /**
     * Updates or adds a new profiling monitor for a particular profile
     *
     * @param profile  the profile to update
     * @param profiler the profile monitor to use
     * @param <T>      the type of the data that will be collected by the {@link PerformanceProfileMonitor}
     */
    public static <T> void setProfiler(PerformanceProfile profile, PerformanceProfileMonitor<T> profiler) {
        profilers.put(profile, profiler);
    }

    /**
     * Returns the current profile monitor tool that's being used by the profile
     *
     * @param profile {@link PerformanceProfile}
     * @return the profile monitor tool
     */
    public static PerformanceProfileMonitor<?> getProfiler(PerformanceProfile profile) {
        return profilers.get(profile);
    }

    /**
     * Fetches the current process information by executing several adb shell commands.
     */
    private static void loadProcessInfo() {
        processInfo = new ProcessInfo();

        final int pid = android.os.Process.myPid();

        String command = "top -b -n 1 -p " + pid;
        String[] commandResult = AdbShellUtils.executeShellCommand(command).split("\n");

        final String[] processInfoString = commandResult[5].trim().split("\\s+");
        final String cpuUsage = processInfoString[8];
        final String memoryUsage = processInfoString[9];

        command = "cat /proc/meminfo";
        commandResult = AdbShellUtils.executeShellCommand(command).split("\n");

        final long memTotal = Long.parseLong(commandResult[0].trim().split("\\s+")[1]);
        final long memFree = Long.parseLong(commandResult[1].trim().split("\\s+")[1]);


        processInfo = ProcessInfo.builder()
                .cpuUsage(Double.parseDouble(cpuUsage))
                .freeSystemMemory(memFree)
                .totalSystemMemory(memTotal)
                .usedSystemMemory(memTotal - memFree)
                .memoryUsage(Double.parseDouble(memoryUsage))
                .packageName(packageName)
                .pid(pid)
                .uid(getApplicationUid()).build();
    }
}
