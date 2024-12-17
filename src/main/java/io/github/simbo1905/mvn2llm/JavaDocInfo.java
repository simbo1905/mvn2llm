package io.github.simbo1905.mvn2llm;

record JavaDocInfo(String fileName, String documentation, String memberSignature) {

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
