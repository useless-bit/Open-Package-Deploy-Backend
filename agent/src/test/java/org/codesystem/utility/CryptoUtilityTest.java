package org.codesystem.utility;

import org.codesystem.PropertiesLoader;
import org.codesystem.TestSystemExitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class CryptoUtilityTest {
    MockedStatic<SystemExitUtility> systemExitMockedStatic;
    PropertiesLoader propertiesLoader;
    CryptoUtility cryptoUtility;

    @BeforeEach
    void setUp() {
        cryptoUtility = null;
        propertiesLoader = Mockito.mock(PropertiesLoader.class);

        systemExitMockedStatic = Mockito.mockStatic(SystemExitUtility.class);
        systemExitMockedStatic.when(() -> SystemExitUtility.exit(Mockito.anyInt())).thenThrow(TestSystemExitException.class);
    }

    @AfterEach
    void tearDown() {
        systemExitMockedStatic.close();
    }

    @Test
    void constructor_invalid() {
        Assertions.assertThrows(TestSystemExitException.class, () -> new CryptoUtility(propertiesLoader));

    }
}