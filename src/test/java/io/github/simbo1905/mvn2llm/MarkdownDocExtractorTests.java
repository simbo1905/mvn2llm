package io.github.simbo1905.mvn2llm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownDocExtractorTests {

  @Test
  void shouldExtractSimpleJavaDoc() {
    var source = """
        /// this is a markdown comment
        public void simpleMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(1)
        .first()
        .satisfies(doc -> {
          assertThat(doc.fileName()).isEqualTo("TestClass");
          assertThat(doc.memberSignature()).isEqualTo("public void simpleMethod() {}");
          assertThat(doc.documentation()).isEqualTo("/// this is a markdown comment");
        });
  }

  @Test
  void shouldExtractMultilineJavaDoc() {
    var source = """
        /// This is a multiline
        /// documentation block
        /// with several lines
        public void multilineMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(1)
        .first()
        .satisfies(doc -> {
          assertThat(doc.memberSignature()).isEqualTo("public void multilineMethod() {}");
          assertThat(doc.documentation()).isEqualTo(
              """
                  /// This is a multiline
                  /// documentation block
                  /// with several lines""");
        });
  }

  @Test
  void shouldHandleBlankLinesBetweenJavaDocAndMethod() {
    var source = """
        
        /// Documentation with
        /// blank lines after
        
        
        public void spacedMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(1)
        .first()
        .satisfies(doc -> {
          assertThat(doc.memberSignature()).isEqualTo("public void spacedMethod() {}");
          assertThat(doc.documentation()).isEqualTo(
              """
                  /// Documentation with
                  /// blank lines after""");
        });
  }

  @Test
  void shouldExtractMultipleJavaDocs() {
    var source = """
        /// First method
        public void firstMethod() {}
        
        /// Second method
        public void secondMethod() {}
        
        /// Third method
        /// with multiple lines
        public void thirdMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(3)
        .satisfies(docList -> {
          assertThat(docList.get(0).documentation()).isEqualTo("/// First method");
          assertThat(docList.get(0).memberSignature()).isEqualTo("public void firstMethod() {}");
          assertThat(docList.get(1).memberSignature()).isEqualTo("public void secondMethod() {}");
          assertThat(docList.get(2).memberSignature()).isEqualTo("public void thirdMethod() {}");
        });
  }


  private List<JavaDocInfo> extractDocs(String source) {
    LinePushStateMachine stateMachine = new LinePushStateMachine("TestClass");
    Arrays.stream(source.split("\n")).forEach(stateMachine::apply);
    return stateMachine.results;
  }

  @Test
  void shouldHandleComplexJavaDoc() {
    var source = """
        /// This is a test case of what complex looks like!
        @SuppressWarnings(value = {
            "one",
            "two"
        }
        )
        @Deprecated(since
            = "Use something else")
        public
        static
        class AnnotatedClass<T>
            implements Function<
            T,
            List<String
                >
            > {
        
          /// This is a field
        
          @SuppressWarnings({
              "unused",
              "unchecked"
          }
          )
          String field;
        
        
          @Override
          public List<String> apply(T t) {
            return List.of();
          }
        
          ///
          /// This is a method
          ///
          @SuppressWarnings({
              "unused",
              "unchecked"
          }
          )
          public
          static <T
              , R>
          List<R> doIt
          (T t) {
            // stuff
            return List.of();
          }
        }
        """;

    var docs = extractDocs(source);

    assertThat(docs).hasSize(3);
    assertThat(docs.getFirst().documentation()).isEqualTo("/// This is a test case of what complex looks like!");
    assertThat(docs.getFirst().vacuum().trim()).isEqualTo(
        """
            @SuppressWarnings(value = { "one", "two" } ) @Deprecated(since = "Use something else") public static class AnnotatedClass<T> implements Function< T, List<String > > {"""
    );
    assertThat(docs.get(1).documentation()).isEqualTo("/// This is a field");
    assertThat(docs.get(1).vacuum().trim()).isEqualTo(
        """
            @SuppressWarnings({ "unused", "unchecked" } ) String field;"""
    );
    assertThat(docs.getLast().documentation()).isEqualTo("""
        ///
        /// This is a method
        ///""");
    assertThat(docs.getLast().vacuum().trim()).isEqualTo(
        """
            @SuppressWarnings({ "unused", "unchecked" } ) public static <T , R> List<R> doIt (T t) {"""
    );
  }

  @Test
  void shouldHandlePackageInfo() {
    var source = """
        /**
         * This is a package info
         */
        package io.github.simbo1905.mvn2llm;
        """;
    final var doc = """
        /**
         * This is a package info
         */
        """.stripIndent();
    final var expected = new JavaDocInfo("TestClass", doc, "package io.github.simbo1905.mvn2llm;");
    final var docs = extractDocs(source);
    assertThat(docs)
        .containsExactly(
            expected
        );
  }

}
