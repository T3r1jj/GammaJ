/* 
 * Copyright 2016 Damian Terlecki.
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
package io.github.t3r1jj.gammaj.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScreenUtilTest {

    public ScreenUtilTest() {
    }

    /**
     * Test of getMonitorsCount method, of class DisplayUtil.
     */
    @Test
    public void testGetMonitorsCount() {
        System.out.println("getMonitorsCount");
        DisplayUtility instance = new DisplayUtility();
        int expResult = 0;
        int result = instance.getMonitorsCount();
        assertTrue(expResult != result);
    }

    /**
     * Test of getMonitor method, of class DisplayUtil.
     */
    @Test
    public void testGetMonitor() {
        System.out.println("getMonitor");
        int id = 0;
        DisplayUtility instance = new DisplayUtility();
        Display result = instance.getMonitor(id);
        assertNotNull(result);
    }

}
