/*
 * SonarQube Java
 * Copyright (C) 2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.java.checks;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.squid.checks.SquidCheck;
import org.sonar.check.BelongsToProfile;
import org.sonar.check.Priority;
import org.sonar.check.Rule;
import org.sonar.java.ast.api.JavaPunctuator;
import org.sonar.java.ast.parser.JavaGrammar;
import org.sonar.sslr.parser.LexerlessGrammar;

import java.util.List;
import java.util.Stack;

@Rule(
  key = "S1166",
  priority = Priority.CRITICAL)
@BelongsToProfile(title = "Sonar way", priority = Priority.CRITICAL)
public class CatchUsesExceptionWithContextCheck extends SquidCheck<LexerlessGrammar> {

  private final Stack<String> exceptionVariables = new Stack<String>();
  private final Stack<Boolean> foundCorrectUsages = new Stack<Boolean>();

  @Override
  public void init() {
    subscribeTo(JavaGrammar.CATCH_CLAUSE);
    subscribeTo(JavaGrammar.ARGUMENTS);
  }

  @Override
  public void visitNode(AstNode node) {
    if (node.is(JavaGrammar.CATCH_CLAUSE) && !isExcluded(node)) {
      exceptionVariables.push(getCaughtVariable(node));
      foundCorrectUsages.push(false);
    } else if (isWithinCatch() && isArgumentsWithSeveralExpressions(node)) {
      for (AstNode expression : node.getChildren(JavaGrammar.EXPRESSION)) {
        if (isExceptionVariable(expression)) {
          foundCorrectUsages.pop();
          foundCorrectUsages.push(true);
        }
      }
    }
  }

  @Override
  public void leaveNode(AstNode node) {
    if (node.is(JavaGrammar.CATCH_CLAUSE) && !isExcluded(node)) {
      exceptionVariables.pop();
      boolean foundCorrectUsage = foundCorrectUsages.pop();

      if (!foundCorrectUsage) {
        getContext().createLineViolation(this, "Either log or rethrow this exception along with some contextual information.", node);
      }
    }
  }

  private boolean isWithinCatch() {
    return !exceptionVariables.isEmpty();
  }

  private static boolean isArgumentsWithSeveralExpressions(AstNode node) {
    return node.hasDirectChildren(JavaPunctuator.COMMA);
  }

  private boolean isExceptionVariable(AstNode node) {
    return hasSingleToken(node) && node.getTokenValue().equals(exceptionVariables.peek());
  }

  private static boolean hasSingleToken(AstNode node) {
    return node.getToken().equals(node.getLastToken());
  }

  private static boolean isExcluded(AstNode node) {
    List<AstNode> blockStatements = node.getFirstChild(JavaGrammar.BLOCK)
      .getFirstChild(JavaGrammar.BLOCK_STATEMENTS)
      .getChildren(JavaGrammar.BLOCK_STATEMENT);

    if (blockStatements.size() != 1) {
      return false;
    }

    AstNode blockStatement = blockStatements.get(0);
    AstNode throwStatement = getThrowStatement(blockStatement);
    if (throwStatement == null) {
      return false;
    }

    String caughtVariable = getCaughtVariable(node);

    return isPropagation(node, caughtVariable, throwStatement) || isCheckedToUncheckedConversion(caughtVariable, blockStatement);
  }

  private static AstNode getThrowStatement(AstNode blockStatement) {
    AstNode statement = blockStatement.getFirstChild(JavaGrammar.STATEMENT);
    if (statement == null) {
      return null;
    }

    AstNode throwStatement = statement.getFirstChild(JavaGrammar.THROW_STATEMENT);
    if (throwStatement == null) {
      return null;
    }

    return throwStatement;
  }

  private static String getCaughtVariable(AstNode catchClause) {
    return catchClause.getFirstChild(JavaGrammar.CATCH_FORMAL_PARAMETER)
      .getFirstChild(JavaGrammar.VARIABLE_DECLARATOR_ID)
      .getTokenOriginalValue();
  }

  private static boolean isPropagation(AstNode catchClause, String caughtVariable, AstNode throwStatement) {
    return !isLastCatch(catchClause) && isRethrowStatement(caughtVariable, throwStatement);
  }

  private static boolean isCheckedToUncheckedConversion(String caughtVariable, AstNode throwStatement) {
    for (AstNode arguments : throwStatement.getDescendants(JavaGrammar.ARGUMENTS)) {
      for (AstNode expression : arguments.getChildren(JavaGrammar.EXPRESSION)) {
        if (AstNodeTokensMatcher.matches(expression, caughtVariable)) {
          return true;
        }
      }
    }

    return false;
  }

  private static boolean isLastCatch(AstNode node) {
    AstNode nextSibling = node.getNextSibling();
    return nextSibling == null ||
      !nextSibling.is(JavaGrammar.CATCH_CLAUSE);
  }

  private static boolean isRethrowStatement(String caughtVariable, AstNode throwStatement) {
    return caughtVariable.equals(getThrownVariable(throwStatement));
  }

  private static String getThrownVariable(AstNode throwStatement) {
    AstNode expression = throwStatement.getFirstChild(JavaGrammar.EXPRESSION);

    return expression.getToken().equals(expression.getLastToken()) ? expression.getTokenOriginalValue() : "";
  }

}
