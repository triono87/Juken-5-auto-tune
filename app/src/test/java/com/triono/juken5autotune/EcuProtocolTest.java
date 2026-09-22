package com.triono.juken5autotune;

import org.junit.Test;

import static org.junit.Assert.*;

public class EcuProtocolTest {
    @Test public void rowAndColumnMapping() {
        assertEquals(0, EcuProtocol.rowForRpm(1000));
        assertEquals(60, EcuProtocol.rowForRpm(16000));
        assertEquals(0, EcuProtocol.colForTps(0));
        assertEquals(20, EcuProtocol.colForTps(100));
    }

    @Test public void parseLiveFrame() {
        String line = "A603;5;13.8;0;3500;900;100;13.20;100;10;250;100;250";
        EcuProtocol.LiveData d = EcuProtocol.parseLiveLine(line);
        assertNotNull(d);
        assertEquals(3500, d.rpm);
        assertEquals(5, d.tps);
        assertEquals(13.20f, d.afr, 0.001f);
    }

    @Test public void rejectMalformedLiveFrame() {
        assertNull(EcuProtocol.parseLiveLine("A603;1;2"));
    }
}
