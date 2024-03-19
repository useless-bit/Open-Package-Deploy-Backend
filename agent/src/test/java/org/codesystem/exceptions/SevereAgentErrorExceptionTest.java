package org.codesystem.exceptions;

import org.codesystem.SystemExit;
import org.codesystem.TestSystemExitException;
import org.junit.jupiter.api.AfterEach;
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
        systemExitMockedStatic.when(() -> SystemExit.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
    }

    @AfterEach
    void teardown() {
        systemExitMockedStatic.close();
    }

    @Test
    void severeAgentErrorException() {
        Assertions.assertThrows(TestSystemExitException.class, () -> new SevereAgentErrorException("Test error"));
    }
}