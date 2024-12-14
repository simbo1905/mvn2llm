package io.github.simbo1905.mvn2llm;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

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
          assertThat(doc.className()).isEqualTo("TestClass");
          assertThat(doc.methodName()).isEqualTo("public void simpleMethod() {}");
          assertThat(doc.documentation()).isEqualTo("/** Simple method description */");
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
          assertThat(doc.methodName()).isEqualTo("public void multilineMethod() {}");
          assertThat(doc.documentation()).isEqualTo(
              "/**\n" +
                  " * This is a multiline\n" +
                  " * documentation block\n" +
                  " * with several lines\n" +
                  " */");
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
          assertThat(doc.methodName()).isEqualTo("public void spacedMethod() {}");
          assertThat(doc.documentation()).isEqualTo(
              "/**\n" +
                  " * Documentation with\n" +
                  " * blank lines after\n" +
                  " */");
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
          assertThat(docList.get(0).methodName()).isEqualTo("public void firstMethod() {}");
          assertThat(docList.get(1).methodName()).isEqualTo("public void secondMethod() {}");
          assertThat(docList.get(2).methodName()).isEqualTo("public void thirdMethod() {}");
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
          assertThat(doc.methodName()).isEqualTo("public void documentedMethod() {}");
          assertThat(doc.documentation()).isEqualTo("/** Real JavaDoc */");
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
    return JavaDocExtractor.extractJavaDoc(
        new BufferedReader(new StringReader(source)),
        "TestClass"
    ).toList();
  }

  @Test
  void testCommentBlockCollector() {
    List<String> input = Arrays.asList(
        "hello",
        "  /** blah",
        "   blah*/",
        "stuff",
        "/** hello ",
        " world ",
        " again */",
        "something"
    );

    List<JavaDocInfo> result = input.stream().collect(new CommentBlockCollector("ClassName"));
    assertThat(result).containsExactly(
        new JavaDocInfo("ClassName", "stuff", "  /** blah\n   blah*/"),
        new JavaDocInfo("ClassName", "something", "/** hello \n world \n again */")
    );
  }

}
