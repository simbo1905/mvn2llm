package io.github.simbo1905.mvn2llm;

import java.util.List;

record JavaDocInfo(String className, String methodName, String documentation) {
  JavaDocInfo(String className, List<String> extracted) {
    this(className,
        extracted.getLast(),
        String.join("\n", extracted.subList(0, extracted.size() - 1)));
  }

  @Override
  public String toString() {
    return """
        Class: %s
        JavaDoc:
        %s
        %s
        """.formatted(className, documentation, methodName);
  }
}
