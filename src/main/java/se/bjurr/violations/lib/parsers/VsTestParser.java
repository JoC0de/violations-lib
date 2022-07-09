package se.bjurr.violations.lib.parsers;

import static java.nio.charset.StandardCharsets.UTF_8;
import static se.bjurr.violations.lib.model.SEVERITY.ERROR;
import static se.bjurr.violations.lib.model.Violation.violationBuilder;
import static se.bjurr.violations.lib.reports.Parser.VSTEST;
import static se.bjurr.violations.lib.util.ViolationParserUtils.findAttribute;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import se.bjurr.violations.lib.model.Violation;
import se.bjurr.violations.lib.util.ViolationParserUtils;
import se.bjurr.violations.lib.ViolationsLogger;

public class VsTestParser implements ViolationsParser {

  private static final Integer MAX_STACK_TRACE_LENGTH = 512;
  private static final Integer MAX_MESSAGE_LENGTH = 512;

  @Override
  public Set<Violation> parseReportOutput(
      final String string, final ViolationsLogger violationsLogger) throws Exception {
    final Set<Violation> violations = new TreeSet<>();

    // remove invalid xml chars like &#x2;
    final String cleaned = string.replaceAll("&#x[\\dA-Fa-f]+;", "");
    try (InputStream input = new ByteArrayInputStream(cleaned.getBytes(UTF_8))) {
      final XMLStreamReader xmlr = ViolationParserUtils.createXmlReader(input);
      final Map<String, String> specifics = new HashMap<>();
      String file = null;
      String message = null;
      int step = 0;
      int lineWithTest = 0;
      String testName = null;
      String testMethodSignature = null;
      String stackTrace = null;

      // regex for stack trace
      // capture groups: 1 -> method signature, 2 -> file path, 3 -> line number
      final Pattern stackTracePattern = Pattern
          .compile("^\\s*\\w+\\s+([^(]+\\([^)]*\\))\\s+\\w+\\s+(.+):line\\s+(\\d+)", Pattern.MULTILINE);
      while (xmlr.hasNext()) {
        final int eventType = xmlr.next();
        if (eventType == XMLStreamConstants.END_ELEMENT && xmlr.getLocalName().equalsIgnoreCase("Results")) {
          break;
        }

        if (eventType == XMLStreamConstants.START_ELEMENT) {
          if (xmlr.getLocalName().equalsIgnoreCase("UnitTestResult")) {
            final boolean isFailedTest = findAttribute(xmlr, "outcome")
                .map(outcome -> outcome.equalsIgnoreCase("Failed"))
                .orElse(false);
            file = null;
            message = null;
            step = 0;
            lineWithTest = 0;
            testName = null;
            testMethodSignature = null;
            stackTrace = null;
            specifics.clear();
            if (isFailedTest) {
              step = 1;
              testName = findAttribute(xmlr, "testName").orElse("UNKNOWN");
              specifics.put("testName", testName);
            }
          } else if (step == 1 && xmlr.getLocalName().equalsIgnoreCase("Output")) {
            step = 2;
          } else if (step == 2 && xmlr.getLocalName().equalsIgnoreCase("ErrorInfo")) {
            step = 3;
          } else if (step == 3 && xmlr.getLocalName().equalsIgnoreCase("Message")) {
            message = xmlr.getElementText().replace("\r", "").trim();
          } else if (step == 3 && xmlr.getLocalName().equalsIgnoreCase("StackTrace")) {
            stackTrace = xmlr.getElementText().replace("\r", "");
            final Matcher matcher = stackTracePattern.matcher(stackTrace);
            if (matcher.find()) {
              specifics.put("failureLocation", matcher.group());
              specifics.put("failedMethod", matcher.group(1));

              // goto last match because it contains the line where the test is defined
              MatchResult lastMatch;
              do {
                lastMatch = matcher.toMatchResult();
              } while (matcher.find());

              testMethodSignature = lastMatch.group(1);
              file = lastMatch.group(2);
              lineWithTest = Integer.parseInt(lastMatch.group(3));
            }
          }
        } else if (step > 1 && eventType == XMLStreamConstants.END_ELEMENT
            && xmlr.getLocalName().equalsIgnoreCase("UnitTestResult")) {
          file = file == null ? testName : file;
          final StringBuilder description = new StringBuilder();
          description.append("Test: ");
          description.append(testName);
          description.append(" failed");
          if (message != null) {
            description.append("\nwith message:\n");
            if (message.length() > MAX_MESSAGE_LENGTH) {
              description.append(message.subSequence(0, MAX_MESSAGE_LENGTH));
              description.append(" [TRUNCATED]");
            } else {
              description.append(message);
            }
          }

          if (stackTrace != null) {
            description.append("\n\nstack trace:\n");
            if (stackTrace.length() > MAX_STACK_TRACE_LENGTH) {
              description.append(stackTrace.subSequence(0, MAX_STACK_TRACE_LENGTH));
              description.append(" [TRUNCATED]");
            } else {
              description.append(stackTrace);
            }
          }

          violations.add( //
              violationBuilder() //
                  .setFile(file) //
                  .setMessage(description.toString()) //
                  .setParser(VSTEST) //
                  .setSource(testMethodSignature) //
                  .setSeverity(ERROR) //
                  .setSpecifics(specifics) //
                  .setStartLine(lineWithTest) //
                  .build() //
          );
        }
      }
    }
    return violations;
  }
}
