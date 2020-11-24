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
package com.microsoft.identity.client.ui.automation.utils;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * This class contains utility methods that can be used to interact with the ADB Shell from within
 * code during the execution of a UI Test.
 */
public class AdbShellUtils {

    public static String executeShellCommand(@NonNull final String command) {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        try {
            return device.executeShellCommand(command);
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    public static String executeShellCommandAsCurrentPackage(@NonNull final String command) {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());
        try {
            final String pkg = ApplicationProvider.getApplicationContext().getPackageName();
            final String completeCmd = "run-as " + pkg + " " + command;
            return device.executeShellCommand(completeCmd);
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
            return null;
        }
    }

    /**
     * Installs the supplied package on the device.
     *
     * @param packageName the name of the package to install
     */
    public static void installPackage(@NonNull final String packageName) {
        final String result = executeShellCommand("pm install " + packageName);
        Assert.assertNotNull(result);
        Assert.assertEquals("Success", result.trim());
    }

    /**
     * Installs the supplied package on the device with the supplied flags.
     *
     * @param packageName the name of the package to install
     * @param flags       the flags to use during installation of the app
     */
    public static void installPackage(@NonNull final String packageName, @NonNull String... flags) {
        final StringBuilder installCmdBuilder = new StringBuilder();
        installCmdBuilder.append("pm install ");

        for (final String flag : flags) {
            installCmdBuilder.append(flag);
            installCmdBuilder.append(" ");
        }

        installCmdBuilder.append(packageName);
        final String result = executeShellCommand(installCmdBuilder.toString());
        Assert.assertNotNull(result);
        Assert.assertEquals("Success", result.trim());
    }

    /**
     * Remove the supplied package name from the device.
     *
     * @param packageName the package name to remove
     */
    public static void removePackage(@NonNull final String packageName) {
        executeShellCommand("pm uninstall " + packageName);
    }

    /**
     * Clear the contents of the storage associated to the given package name.
     *
     * @param packageName the package name to clear
     */
    public static void clearPackage(@NonNull final String packageName) {
        executeShellCommand("pm clear " + packageName);
    }

    /**
     * Force stop (shut down) the supplied the package.
     *
     * @param packageName the package to force stop
     */
    public static void forceStopPackage(@NonNull final String packageName) {
        executeShellCommand("am force-stop " + packageName);
    }

    private static void putGlobalSettings(final String settingName, final String value) {
        executeShellCommand("settings put global " + settingName + " " + value);
    }

    /**
     * Enable automatic time zone on the device.
     */
    public static void enableAutomaticTimeZone() {
        putGlobalSettings("auto_time", "1");
    }

    /**
     * Disable automatic time zone on the device.
     */
    public static void disableAutomaticTimeZone() {
        putGlobalSettings("auto_time", "0");
    }

    private static String getApkPath(@NonNull final String packageName) {
        return executeShellCommand("pm path " + packageName);
    }

    /**
     * Copy APK of the specified package to the specified location.
     *
     * @param packageName     the package name of the APK to copy
     * @param destApkFileName the destination location where to copy the APK
     */
    public static void copyApkForPackage(@NonNull final String packageName,
                                         @NonNull final String destApkFileName) {
        final String apkPath = getApkPath(packageName);
        final String sanitizedPath = apkPath.trim().replace("package:", "");
        copyFile(sanitizedPath, destApkFileName);
    }

    /**
     * Copy a file to the specified destination.
     *
     * @param srcFile  the file to copy
     * @param destFile the destination location where to copy the file
     */
    public static void copyFile(@NonNull final String srcFile, @NonNull final String destFile) {
        executeShellCommand("cp " + srcFile + " " + destFile);
    }

    public static void copyToSdCard(@NonNull final String srcFile, @NonNull final String destFile) {
        executeShellCommand("mkdir /sdcard/automation");
        executeShellCommandAsCurrentPackage("cp " + srcFile + " " + destFile);
    }
}
