/*
 * Copyright 2016 Joe Rogers
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

package com.example.crudtester.task;

import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.test.mock.MockContentResolver;

/**
 * Isolated context that configures and installs the mock content provider
 */
public class TaskMockContext extends ContextWrapper {

    private final MockContentResolver contentResolver;

    public TaskMockContext(String authority, Context context) {
        super(context);
        contentResolver = buildContentResolver(authority);
    }

    private static MockContentResolver buildContentResolver(String authority) {
        MockContentResolver contentResolver = new MockContentResolver();
        contentResolver.addProvider(authority, new ServiceMockContentProvider());
        return contentResolver;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public ContentResolver getContentResolver() {
        return contentResolver;
    }
}
