package io.github.simbo1905.mvn2llm;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static io.github.simbo1905.mvn2llm.LinePushStateMachine.endOfMemberSignature;
import static org.assertj.core.api.Assertions.assertThat;

class JavaDocExtractorTests {

  @Test
  void shouldExtractSimpleJavaDoc() {
    var source = """
        /** Simple method description */
        public void simpleMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(1)
        .first()
        .satisfies(doc -> {
          assertThat(doc.fileName()).isEqualTo("TestClass");
          assertThat(doc.memberSignature()).isEqualTo("public void simpleMethod() {}");
          assertThat(doc.documentation()).isEqualTo("/** Simple method description */" +
              "\n");
        });
  }

  @Test
  void shouldExtractMultilineJavaDoc() {
    var source = """
        /**
         * This is a multiline
         * documentation block
         * with several lines
         */
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
                  /**
                   * This is a multiline
                   * documentation block
                   * with several lines
                   */
                  """);
        });
  }

  @Test
  void shouldHandleBlankLinesBetweenJavaDocAndMethod() {
    var source = """
        /**
         * Documentation with
         * blank lines after
         */
        
        
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
                  /**
                   * Documentation with
                   * blank lines after
                   */
                  """);
        });
  }

  @Test
  void shouldExtractMultipleJavaDocs() {
    var source = """
        /** First method */
        public void firstMethod() {}
        
        /** Second method */
        public void secondMethod() {}
        
        /**
         * Third method
         * with multiple lines
         */
        public void thirdMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(3)
        .satisfies(docList -> {
          assertThat(docList.get(0).memberSignature()).isEqualTo("public void firstMethod() {}");
          assertThat(docList.get(1).memberSignature()).isEqualTo("public void secondMethod() {}");
          assertThat(docList.get(2).memberSignature()).isEqualTo("public void thirdMethod() {}");
        });
  }

  @Test
  void shouldIgnoreNonJavaDocComments() {
    var source = """
        // Regular comment
        public void ignoredMethod1() {}
        
        /* Block comment */
        public void ignoredMethod2() {}
        
        /** Real JavaDoc */
        public void documentedMethod() {}
        """;

    var docs = extractDocs(source);

    assertThat(docs)
        .hasSize(1)
        .first()
        .satisfies(doc -> {
          assertThat(doc.memberSignature()).isEqualTo("public void documentedMethod() {}");
          assertThat(doc.documentation()).isEqualTo("/** Real JavaDoc */\n");
        });
  }

  @Test
  void shouldHandleEmptyFile() {
    var docs = extractDocs("");
    assertThat(docs).isEmpty();
  }

  @Test
  void shouldHandleFileWithNoJavaDoc() {
    var source = """
        public class NoJavaDoc {
            public void method1() {}
            public void method2() {}
        }
        """;

    var docs = extractDocs(source);
    assertThat(docs).isEmpty();
  }

  private List<JavaDocInfo> extractDocs(String source) {
    LinePushStateMachine stateMachine = new LinePushStateMachine("TestClass");
    Arrays.stream(source.split("\n")).forEach(stateMachine::apply);
    return stateMachine.results;
  }

  /**
   * This is a test case of what complex looks like!
   */
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
    /**
     * This is a field
     */
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

    /**
     * This is a method
     */
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

  @Test
  void testEndOfMemberSignatureWithAnnotations() {
    // test field with new lines for semicolon shortcut
    assert endOfMemberSignature("""
        public
         final
         String
         field = "value";
        """);
    // test the sami colon shortcut
    assert endOfMemberSignature("public void method(@NotNull String arg){};");
    // test class with shortcut
    assert endOfMemberSignature("public interface MakerInterface{};");
    // test method param with annotation
    assert endOfMemberSignature("public void method(@NotNull String arg){");
    // test method param with annotation and comment
    assert endOfMemberSignature("public void method(@NotNull String arg){ // comment");
    // test method param with annotation with value
    assert endOfMemberSignature("""
        public void method(@Value("default") String arg){ // comment
        """);
    // test method param with annotation with array of values
    assert endOfMemberSignature("""
        public void method(@Value({"a","b"}) String arg){ // comment
        """);
    // test method param with many annotations
    assert endOfMemberSignature("""
        public void method(@Value({"a","b"}) String arg1, @Value({"c","d"}) String arg2){ // comment
        """);
    // test method param with many annotations and method annotation
    assert endOfMemberSignature("""
        @NotNull
        public void method(@Value({"a","b"}) String arg1, @Value({"c","d"}) String arg2){ // comment
        """);
    // test method param with many annotations and method annotation with value
    assert endOfMemberSignature("""
        @Deprecated(since="1.1")
        public void method(@Value({"a","b"}) String arg1, @Value({"c","d"}) String arg2){ // comment
        """);
    // test method param with many annotations and method annotation with array
    assert endOfMemberSignature("""
        @SuppressWarning({"unused","unchecked"})
        public
          void
            method(
        @Value(
        {
        "a"
        ,"b"})
         String arg1, @Value
          (
          {"c","d"
          }) String arg2){ // comment
        """);
    // test class with basic annotation
    assert endOfMemberSignature("@Deprecated public class MyClass{");
    // test class with annotation with value
    assert endOfMemberSignature("@Deprecated(since=\"1.1\") public class MyClass{");
    // test class with annotation with array
    assert endOfMemberSignature("@SuppressWarning({\"unused\",\"unchecked\"}) public class MyClass{");
    assert endOfMemberSignature("""
        @SuppressWarning(
            {
            "unused",
            "unchecked"
            }
        )
        @Deprecated(
            since="1.1"
        )
        public
        class
        MyClass
        {
        """);
  }


  @Test
  void shouldHandleComplexJavaDoc() {
    var source = """
        /**
         * This is a test case of what complex looks like!
         */
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
          /**
           * This is a field
           */
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
        
          /**
           * This is a method
           */
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
    docs.forEach(doc -> {
      assertThat(doc.documentation().trim()).startsWith("/**");
    });
    docs.forEach(doc -> {
      assertThat(doc.documentation().trim()).endsWith("*/");
    });
    assertThat(docs.getFirst().vacuum().trim()).isEqualTo(
        """
            @SuppressWarnings(value = { "one", "two" } ) @Deprecated(since = "Use something else") public static class AnnotatedClass<T> implements Function< T, List<String > > {"""
    );
    assertThat(docs.get(1).vacuum().trim()).isEqualTo(
        """
            @SuppressWarnings({ "unused", "unchecked" } ) String field;"""
    );
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
    final var first = docs.getFirst();
    assertThat(docs)
        .containsExactly(
            expected
        );
  }

}
