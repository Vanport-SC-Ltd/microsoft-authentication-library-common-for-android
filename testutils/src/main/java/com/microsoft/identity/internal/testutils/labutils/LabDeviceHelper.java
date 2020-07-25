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
package com.microsoft.identity.internal.testutils.labutils;

import com.microsoft.identity.internal.test.labapi.ApiException;
import com.microsoft.identity.internal.test.labapi.api.DeleteDeviceApi;
import com.microsoft.identity.internal.test.labapi.model.CustomSuccessResponse;

/**
 * Utilities to interact with Lab {@link DeleteDeviceApi}
 */
public class LabDeviceHelper {

    /**
     * Deletes the provided device from the directory
     *
     * @param upn      the upn to whom this device is associated
     * @param deviceId the device id of the device to delete
     * @return
     */
    public static boolean deleteDevice(final String upn, final String deviceId) {
        LabAuthenticationHelper.getInstance().setupApiClientWithAccessToken();
        DeleteDeviceApi deleteDeviceApi = new DeleteDeviceApi();

        final CustomSuccessResponse customSuccessResponse;
        try {
            customSuccessResponse = deleteDeviceApi.delete(upn, deviceId);

            return customSuccessResponse.getResult().contains(
                    deviceId + ", successfully deleted from AAD."
            );
        } catch (ApiException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
