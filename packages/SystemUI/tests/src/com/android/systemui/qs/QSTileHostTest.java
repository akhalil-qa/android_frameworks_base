/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.qs;


import static junit.framework.TestCase.assertFalse;

import android.support.test.filters.SmallTest;
import android.testing.AndroidTestingRunner;

import com.android.systemui.SysuiTestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidTestingRunner.class)
@SmallTest
public class QSTileHostTest extends SysuiTestCase {

    @Test
    public void testLoadTileSpecs_emptySetting() {
        List<String> tiles = QSTileHost.loadTileSpecs(mContext, "");
        assertFalse(tiles.isEmpty());
    }

    @Test
    public void testLoadTileSpecs_nullSetting() {
        List<String> tiles = QSTileHost.loadTileSpecs(mContext, null);
        assertFalse(tiles.isEmpty());
    }
}