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

import com.sonar.sslr.squid.checks.CheckMessagesVerifierRule;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.java.JavaAstScanner;
import org.sonar.java.model.VisitorsBridge;
import org.sonar.squid.api.SourceFile;

import java.io.File;

public class CollectionImplementationReferencedCheckTest {

  @Rule
  public CheckMessagesVerifierRule checkMessagesVerifier = new CheckMessagesVerifierRule();

  @Test
  public void detected() {
    SourceFile file = JavaAstScanner.scanSingleFile(
      new File("src/test/files/checks/CollectionImplementationReferencedCheck.java"),
      new VisitorsBridge(new CollectionImplementationReferencedCheck()));
    checkMessagesVerifier.verify(file.getCheckMessages())
      .next().atLine(2).withMessage("The type of the \"employees\" object should be an interface such as \"Set\" rather than the implementation \"HashSet\".")
      .next().atLine(4).withMessage("The return type of this method should be an interface such as \"Set\" rather than the implementation \"HashSet\".")
      .next().atLine(8).withMessage("The return type of this method should be an interface such as \"List\" rather than the implementation \"LinkedList\".")
      .next().atLine(14).withMessage("The return type of this method should be an interface such as \"Map\" rather than the implementation \"HashMap\".");
  }

}
