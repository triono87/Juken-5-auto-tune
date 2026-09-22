package com.triono.juken5autotune;

import org.junit.Test;

import static org.junit.Assert.*;

public class ProtocolClassifierTest {
    @Test public void recognizesLive() {
        ProtocolMessageClassifier.Message m =
                ProtocolMessageClassifier.classify("A603;1;2;3");
        assertEquals(ProtocolMessageClassifier.Type.LIVE, m.type);
    }

    @Test public void recognizes21CellCandidate() {
        StringBuilder s = new StringBuilder("MAPX");
        for (int i = 0; i < 21; i++) s.append(";").append(100 + i);
        ProtocolMessageClassifier.Message m =
                ProtocolMessageClassifier.classify(s.toString());
        assertEquals(ProtocolMessageClassifier.Type.MAP_CANDIDATE, m.type);
        assertEquals(22, m.fieldCount);
        assertNotNull(MapRowCandidateParser.parse(s.toString(), "MAPX"));
    }

    @Test public void rejectsWrongCellCount() {
        assertFalse(MapRowCandidateParser.is21CellCandidate("MAPX;1;2;3"));
    }
}
