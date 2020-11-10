//
// Copyright (c) 2020 Couchbase, Inc All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.couchbase.android.listsync.p2p;

import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import com.couchbase.android.listsync.db.DatabaseManager;
import com.couchbase.android.listsync.util.ObservableMap;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Replicator;
import com.couchbase.lite.URLEndpointListener;
import com.couchbase.lite.URLEndpointListenerConfiguration;


@Singleton
public final class ListenerManager {
    private static final String TAG = "SYNC";

    @NonNull
    private final Executor syncExecutor = Executors.newSingleThreadExecutor();
    @NonNull
    private final Scheduler syncScheduler = Schedulers.from(syncExecutor);

    @NonNull
    @GuardedBy("listeners")
    private final Map<URI, URLEndpointListener> listeners = new HashMap<>();

    @NonNull
    private final DatabaseManager dbMgr;

    @Inject
    public ListenerManager(@NonNull DatabaseManager dbMgr) { this.dbMgr = dbMgr; }

    @NonNull
    public Set<URI> getServers() {
        synchronized (listeners) { return listeners.keySet(); }
    }

    @UiThread
    @NonNull
    public Observable<Set<URI>> startServer() { return startServer(null, 0); }

    @UiThread
    @NonNull
    public Observable<Set<URI>> startServer(@Nullable String iface, int port) {
        return Observable
            .fromCallable(() -> startServerAsync(iface, port))
            .subscribeOn(syncScheduler)
            .observeOn(AndroidSchedulers.mainThread());
    }

    @UiThread
    @NonNull
    public Observable<Set<URI>> stopServer(@NonNull URI uri) {
        return Observable
            .fromCallable(() -> stopServerAsync(uri))
            .subscribeOn(syncScheduler)
            .observeOn(AndroidSchedulers.mainThread());
    }

    @WorkerThread
    private Set<URI> startServerAsync(@Nullable String iface, int port) throws CouchbaseLiteException {
        final URLEndpointListenerConfiguration config = dbMgr.getListenerConfig();
        if (iface != null) { config.setNetworkInterface(iface); }
        if (port > URLEndpointListenerConfiguration.MIN_PORT) { config.setPort(port); }

        final URLEndpointListener listener = new URLEndpointListener(config);

        Log.i(TAG, "Starting server @" + config);
        listener.start();

        final URI uri = listener.getUrls().get(0);
        addServer(uri, listener);


        return getServers();
    }

    @WorkerThread
    public Set<URI> stopServerAsync(@NonNull URI uri) {
        final URLEndpointListener listener = removeServer(uri);
        if (listener == null) {
            Log.i(TAG, "Attempt to stop non-existent listener: " + uri);
        }
        else {
            Log.d(TAG, "Stopping server @" + listener.getConfig());
            listener.stop();
        }

        return getServers();
    }

    private void addServer(URI uri, URLEndpointListener listener) {
        synchronized (listeners) { listeners.put(uri, listener); }
    }

    @Nullable
    private URLEndpointListener removeServer(URI uri) {
        synchronized (listeners) { return listeners.remove(uri); }
    }
}