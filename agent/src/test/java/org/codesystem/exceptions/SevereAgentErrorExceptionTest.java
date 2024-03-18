package org.codesystem.exceptions;

import org.codesystem.SystemExit;
import org.codesystem.TestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class SevereAgentErrorExceptionTest {
    MockedStatic<SystemExit> systemExitMockedStatic;

    @BeforeEach
    void setup() {
        systemExitMockedStatic = Mockito.mockStatic(SystemExit.class);
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).thenThrow(TestException.class);
    }

    @Test
    void severeAgentErrorException() {
        Assertions.assertThrows(TestException.class, () -> new SevereAgentErrorException("Test error"));
    }
}