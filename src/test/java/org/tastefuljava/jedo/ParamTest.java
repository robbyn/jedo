package org.tastefuljava.jedo;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.tastefuljava.jedo.expression.Parameter;
import org.tastefuljava.jedo.expression.Scope;

public class ParamTest {
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    @Test
    public void testParams() {
        Instant self = Instant.now();
        Scope scope = new Scope.ParameterScope(null, "self");
        Parameter parm = Parameter.parse(scope, "self");
        assertEquals("#0", parm.toString());
        parm = Parameter.parse(scope, "self:12");
        assertEquals("#0:12", parm.toString());
        parm = Parameter.parse(scope, "self:java.sql.Timestamp");
        assertEquals("#0:java.sql.Timestamp", parm.toString());
        parm = Parameter.parse(scope, "self:java.sql.Timestamp");
        assertEquals("#0:java.sql.Timestamp", parm.toString());
        parm = Parameter.parse(scope, "self::TIMESTAMP");
        assertEquals("#0::" + Types.TIMESTAMP, parm.toString());
        parm = Parameter.parse(scope, "self:java.sql.Timestamp:TIMESTAMP:12");
        assertEquals("#0:java.sql.Timestamp:" + Types.TIMESTAMP + ":12",
                parm.toString());
        Object value = parm.evaluate(null, self);
        assertTrue(value instanceof Timestamp);
    }
}
