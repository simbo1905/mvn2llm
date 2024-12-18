package io.github.simbo1905.mvn2llm;

record JavaDocInfo(String fileName, String documentation, String memberSignature) {
  public JavaDocInfo {
    fileName = fileName.strip();
    documentation = documentation.strip();
    memberSignature = memberSignature.strip();
  }

  @Override
  public String toString() {
    return """
        %s
        %s
        %s
        """.formatted(fileName, documentation.trim(), vacuum().trim());
  }

  public String vacuum() {
    return memberSignature.replaceAll("\\s+", " ").trim();
  }
}
