package com.microsoft.identity.common.internal.telemetry;
// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class CacheEvent extends BaseEvent {
    private static final String TAG = CacheEvent.class.getSimpleName();

    public CacheEvent() {
        super();
        putEventName(TELEMETRY_EVENT_CACHE_EVENT);
    }

    /*
    MSID_TELEMETRY_KEY_TOKEN_TYPE
    MSID_TELEMETRY_KEY_IS_FRT
    MSID_TELEMETRY_KEY_IS_MRRT
    MSID_TELEMETRY_KEY_IS_RT

    MSID_TELEMETRY_KEY_RESULT_STATUS
    MSID_TELEMETRY_KEY_RT_STATUS
    MSID_TELEMETRY_KEY_MRRT_STATUS
    MSID_TELEMETRY_KEY_FRT_STATUS
    MSID_TELEMETRY_KEY_SPE_INFO


    MSID_TELEMETRY_KEY_WIPE_APP
    MSID_TELEMETRY_KEY_WIPE_TIME

    */

}
