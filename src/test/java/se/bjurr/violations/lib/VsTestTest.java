package se.bjurr.violations.lib;

import static org.assertj.core.api.Assertions.assertThat;
import static se.bjurr.violations.lib.TestUtils.getRootFolder;
import static se.bjurr.violations.lib.ViolationsApi.violationsApi;
import static se.bjurr.violations.lib.model.SEVERITY.ERROR;
import static se.bjurr.violations.lib.reports.Parser.VSTEST;

import java.util.ArrayList;
import java.util.Set;
import org.junit.Test;
import se.bjurr.violations.lib.model.Violation;

public class VsTestTest {
    @Test
    public void testThatViolationsCanBeParsedFromTestLogger() {
        final String rootFolder = getRootFolder();

        final Set<Violation> actual = violationsApi() //
                .withPattern(".*/vstest/vstest-test-results.trx$") //
                .inFolder(rootFolder) //
                .findAll(VSTEST) //
                .violations();

        assertThat(actual) //
                .hasSize(4);

        final ArrayList<Violation> violations = new ArrayList<>(actual);
        Violation violation = violations.get(0);
        assertThat(violation.getMessage()) //
                .isEqualTo("Test: SupClassTest(2) failed\nwith message:\nExpected: 1\n  But was:  2\n" +
                "stack trace:\n   at vstest.Tests.SupClass.Other(Int32 testData) in C:\\Projects\\xrp_test\\violations-lib\\src\\test\\resources\\vstest\\UnitTest1.cs:line 48\n" +
                "   at vstest.Tests.SupClass.Method(Int32 testData) in C:\\Projects\\xrp_test\\violations-lib\\src\\test\\resources\\vstest\\UnitTest1.cs:line 43\n" +
                "   at vstest.Tests.SupClassTest(Int32 testData) in C:\\Projects\\xrp_test\\violations-lib\\src\\test\\resources\\vstest\\UnitTest1.cs:line 36\n");
        assertThat(violation.getFile()) //
                .isEqualTo("C:/Projects/xrp_test/violations-lib/src/test/resources/vstest/UnitTest1.cs");
        assertThat(violation.getSeverity()) //
                .isEqualTo(ERROR);
        assertThat(violation.getStartLine()) //
                .isEqualTo(36);

        violation = violations.get(1);
        assertThat(violation.getMessage()) //
                .isEqualTo("Test3");
        assertThat(violation.getFile()) //
                .isEqualTo(
                        "C:/Projects/xrp_test/violations-lib/src/test/resources/vstest/UnitTest1.cs");
        assertThat(violation.getSeverity()) //
                .isEqualTo(ERROR);
        assertThat(violation.getStartLine()) //
                .isEqualTo(29);

        violation = violations.get(2);
        assertThat(violation.getMessage()) //
                .isEqualTo("Test2\nSystem.InvalidOperationException : Test-exception");
        assertThat(violation.getFile()) //
                .isEqualTo(
                        "C:/Projects/xrp_test/violations-lib/src/test/resources/vstest/UnitTest1.cs");
        assertThat(violation.getSeverity()) //
                .isEqualTo(ERROR);
        assertThat(violation.getStartLine()) //
                .isEqualTo(22);
                
        violation = violations.get(3);
        assertThat(violation.getMessage()) //
                .isEqualTo("Test1\nExpected: 1\n  But was:  3");
        assertThat(violation.getFile()) //
                .isEqualTo(
                        "c:/jenkins/workspace/dev/Common/Common.Settings.Test/BlackBox/UserStatus/Bb_UserStatus_GetLimitFixedTest.cs");
        assertThat(violation.getSeverity()) //
                .isEqualTo(ERROR);
        assertThat(violation.getStartLine()) //
                .isEqualTo(32);
    }
}
