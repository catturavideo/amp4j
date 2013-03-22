
package org.amp4j;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.amp4j.internet.TestInternet;
import org.amp4j.amp.TestAMP;

public class AllJUnitTests {
    public static Test suite() {
        TestSuite ts = new TestSuite();
        for (Test t: new Test[]
            {
                TestInternet.suite(),
                TestAMP.suite()
            }) {
            ts.addTest(t);
        }
        return ts;
    }
}

