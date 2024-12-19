package io.github.simbo1905.mvn2llm;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class SnapshotMetadataParser extends DefaultHandler {
  private final StringBuilder currentValue = new StringBuilder();
  private String classifier = null;
  private String extension = null;
  private String value = null;
  private String timestamp = null;
  private String buildNumber = null;
  private boolean inSnapshotVersion = false;
  private String sourcesJarVersion = null;

  static String parseSnapshotVersion(String xml) throws Exception {
    final var parser = SAXParserFactory.newInstance().newSAXParser();
    final var handler = new SnapshotMetadataParser();
    parser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), handler);
    return handler.getSourcesJarVersion();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) {
    currentValue.setLength(0);
    if ("snapshotVersion".equals(qName)) {
      inSnapshotVersion = true;
      classifier = null;
      extension = null;
      value = null;
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) {
    currentValue.append(ch, start, length);
  }

  @Override
  public void endElement(String uri, String localName, String qName) {
    final var elementValue = currentValue.toString().trim();

    switch (qName) {
      case "classifier" -> classifier = elementValue;
      case "extension" -> extension = elementValue;
      case "value" -> value = elementValue;
      case "timestamp" -> timestamp = elementValue;
      case "buildNumber" -> buildNumber = elementValue;
      case "snapshotVersion" -> {
        if (inSnapshotVersion &&
            "sources".equals(classifier) &&
            "jar".equals(extension) &&
            value != null) {
          sourcesJarVersion = value;
        }
        inSnapshotVersion = false;
      }
    }
  }

  String getSourcesJarVersion() {
    if (sourcesJarVersion != null) {
      return sourcesJarVersion;
    }
    // Fallback to constructing from timestamp and buildNumber if available
    if (timestamp != null && buildNumber != null) {
      return "6.2.2-%s-%s".formatted(timestamp, buildNumber);
    }
    throw new IllegalStateException("No sources jar version found in metadata");
  }
}
