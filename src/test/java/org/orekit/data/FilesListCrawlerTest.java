/* Copyright 2002-2025 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.orekit.errors.OrekitException;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class FilesListCrawlerTest extends AbstractListCrawlerTest<File> {

    protected File input(String resource) {
        try {
            return new File(FilesListCrawlerTest.class.getClassLoader().getResource(resource).toURI().getPath());
        } catch (URISyntaxException ue) {
            Assertions.fail(ue.getLocalizedMessage());
            return null;
        }
    }

    protected FilesListCrawler build(String... inputs) {
        File[] converted = new File[inputs.length];
        for (int i = 0; i < inputs.length; ++i) {
            converted[i] = input(inputs[i]);
        }
        return new FilesListCrawler(converted);
    }

    @Test
    public void noElement() {
        try {
            File existing   = new File(input("regular-data").getPath());
            File inexistent = new File(existing.getParent(), "inexistant-directory");
            new FilesListCrawler(inexistent).feed(Pattern.compile(".*"), new CountingLoader(),
                                                  DataContext.getDefault().getDataProvidersManager());
            Assertions.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assertions.assertInstanceOf(FileNotFoundException.class, oe.getCause());
            Assertions.assertTrue(oe.getLocalizedMessage().contains("inexistant-directory"));
        }
   }

}
