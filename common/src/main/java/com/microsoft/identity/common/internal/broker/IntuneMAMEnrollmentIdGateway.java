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
package com.microsoft.identity.common.internal.broker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.util.LruCache;

import com.microsoft.identity.common.logging.Logger;

import lombok.EqualsAndHashCode;
import lombok.NonNull;

/**
 * Utility class to retrieve the Intune MAM enrollment id from the Intune
 * Company Portal, if available.  The enrollment id is required for token
 * acquisition for MAM-CA protected resources.
 */
public class IntuneMAMEnrollmentIdGateway {
    static IntuneMAMEnrollmentIdGateway sInstance;

    private static String CONTENT_URI = "content://com.microsoft.intune.mam.policy/mamserviceenrollments";
    private static String SELECTION = "WHERE PackageName = ? AND Identity = ?";
    private static String[] PROJECTION = {"EnrollmentId"};

    private static final String TAG = "IntuneMAMEnrollmentIdGateway";

    // entries live in the cache for 30 seconds
    private static final long CACHE_TTL_MS = 30000;
    // the cache size is somewhat arbitrary, but we don't want entries
    // kicked out before their 30 second TTL is up.  30 seems like a
    // reasonable number of userId/packageName combinations in a
    // 30 second period.
    private static final int CACHE_SIZE = 30;
    // to handle the case where an app becomes enrolled, we need to
    // return the enrollment id sooner than 30 seconds since the last
    // request.  This somewhat degrades the effect of caching for apps
    // that never become enrolled, but the alternative is a 30 second
    // delay before an app can successfully acquire a token after
    // becoming compliant.  1.5 seconds is a guess for a reasonable
    // time to become compliant and re-request the token.
    private static final long CACHE_TTL_FOR_NULLS_MS = 1500;

    @EqualsAndHashCode
    private static final class CacheKey {
        public final String userId;
        public final String packageName;

        public CacheKey(@NonNull final String userId, @NonNull final String packageName) {
            // note: keep values in lower case so string comparisons
            // don't have to be case-insensitive.
            this.userId = userId.toLowerCase();
            this.packageName = packageName.toLowerCase();
        }
    }

    private static final class CacheEntry {
        public final String enrollmentId;
        public final long timestampMs;

        public CacheEntry(String enrollmentId) {
            this.enrollmentId = enrollmentId;
            this.timestampMs = SystemClock.elapsedRealtime();
        }

        public boolean expired() {
            final long timeoutMs = (enrollmentId == null) ? CACHE_TTL_FOR_NULLS_MS : CACHE_TTL_MS;
            return (SystemClock.elapsedRealtime() - this.timestampMs > timeoutMs);
        }
    }

    private final LruCache<CacheKey, CacheEntry> mIdCache;

    /**
     * Singleton implementation for IntuneMAMEnrollmentIdGateway.
     *
     * @return IntuneMAMEnrollmentIdGateway singleton instance
     */
    public static synchronized IntuneMAMEnrollmentIdGateway getInstance() {
        if (sInstance == null) {
            sInstance = new IntuneMAMEnrollmentIdGateway();
        }
        return sInstance;
    }

    private IntuneMAMEnrollmentIdGateway() {
        mIdCache = new LruCache<>(CACHE_SIZE);
    }

    /**
     * Retrieve the Intune MAM enrollment id for the given user and package from
     * the Intune Company Portal, if available.
     *
     * @param context     the context, used to get a content resolver.
     * @param userId      object id of the user for whom a token is being acquired.
     * @param packageName name of the package requesting the token.
     * @return the enrollment id, or null if enrollment id can't be retrieved.
     */
    public synchronized String getEnrollmentId(final Context context, final String userId, final String packageName) {
        // first look in the cache
        final CacheKey key = new CacheKey(userId, packageName);
        CacheEntry entry = mIdCache.get(key);
        if (entry == null || entry.expired()) {
            final String enrollmentId = callContentProvider(context, userId, packageName);
            entry = new CacheEntry(enrollmentId);
            mIdCache.put(key, entry);
        }

        return entry.enrollmentId;
    }

    private String callContentProvider(final Context context, final String userId, final String packageName) {
        final String[] selectionArgs = {packageName, userId};
        final Uri contentURI = Uri.parse(CONTENT_URI);

        String result = null;
        try {
            final Cursor found = context.getContentResolver().query(contentURI, PROJECTION,
                    SELECTION, selectionArgs, null);

            if (found != null) {
                if (found.moveToFirst()) {
                    result = found.getString(0);
                }

                found.close();
            } else{
                Logger.verbose(TAG + " callContentProvider", "Cursor was null.  The content provider may not be available. ");
            }
        } catch (final Exception e) {
            // We don't expect this to fail, since the implementation in the Company Portal
            // returns nulls instead of throwing exceptions.  This is a safety measure in
            // case the implementation changes or if there is a bug on the Company Portal side.
            Logger.warn(TAG, "Unable to query enrollment id: " + e.getMessage());
        }
        return result;
    }
}
