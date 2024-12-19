package io.github.simbo1905.mvn2llm;

record MavenCoordinate(String groupId, String artifactId, String version) {
  static MavenCoordinate parse(String input) {
    final var parts = input.split(":");
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid coordinate format. Expected: groupId:artifactId:version");
    }
    return new MavenCoordinate(parts[0], parts[1], parts[2]);
  }

  String toPath() {
    return "%s/%s/%s/%s-%s-sources.jar".formatted(
        groupId.replace('.', '/'),
        artifactId,
        version,
        artifactId,
        version
    );
  }
}
