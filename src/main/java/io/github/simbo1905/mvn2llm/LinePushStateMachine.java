package io.github.simbo1905.mvn2llm;

import java.util.ArrayList;
import java.util.List;

public class LinePushStateMachine {
  private final String fileName;
  List<JavaDocInfo> results = new ArrayList<>();

  public LinePushStateMachine(String fileName) {
    this.fileName = fileName;
  }

  enum State {
    START,
    IN_JAVADOC,
    IN_MEMBER_SIGNATURE,
  }

  StringBuilder javadoc = new StringBuilder();
  StringBuilder memberSignature = new StringBuilder();
  State state = State.START;

  void apply(String line) {
    final var trimmed = line.trim();
    switch (state) {
      case START -> {
        if (trimmed.startsWith("/**") && trimmed.contains("*/")) {
          javadoc.append(line).append("\n");
          state = State.IN_MEMBER_SIGNATURE;
        } else if (trimmed.startsWith("/**")) {
          state = State.IN_JAVADOC;
          javadoc.append(line).append("\n");
        } else if (trimmed.startsWith("///")) {
          state = State.IN_JAVADOC;
          javadoc.append(line).append("\n");
        }
      }
      case IN_JAVADOC -> {
        javadoc.append(line).append("\n");
        if (trimmed.startsWith("*/")) {
          state = State.IN_MEMBER_SIGNATURE;
        }
      }
      case IN_MEMBER_SIGNATURE -> {
        memberSignature.append(line);
        if (endOfMemberSignature(memberSignature.toString())) {
          results.add(new JavaDocInfo(fileName, javadoc.toString(), memberSignature.toString().trim()));
          state = State.START;
          javadoc = new StringBuilder();
          memberSignature = new StringBuilder();
        } else {
          memberSignature.append(" ");
        }
      }
    }
  }

  /// Java will eventually have withers to remove this boilerplate
  record ParsingState(boolean insideParens, boolean foundUnenclosedBrace) {
    static ParsingState initial() {
      return new ParsingState(false, false);
    }

    ParsingState withParensState(boolean newParensState) {
      return new ParsingState(newParensState, foundUnenclosedBrace);
    }

    ParsingState withFoundBrace() {
      return new ParsingState(insideParens, true);
    }
  }

  static boolean endOfMemberSignature(String input) {
    // fields, method signatures, class signatures must have ended by the time we hit a semicolon
    if (input.contains(";")) {
      return true;
    }

    return input.chars()
        .mapToObj(ch -> (char) ch)
        .reduce(
            ParsingState.initial(),
            (state, ch) -> switch (ch) {
              case '(' -> state.withParensState(true);
              case ')' -> state.withParensState(false);
              case '{' -> state.insideParens ? state : state.withFoundBrace();
              default -> state;
            },
            (state1, state2) -> new ParsingState(
                state1.insideParens() || state2.insideParens(),
                state1.foundUnenclosedBrace() || state2.foundUnenclosedBrace()
            )
        )
        .foundUnenclosedBrace();

  }
}
