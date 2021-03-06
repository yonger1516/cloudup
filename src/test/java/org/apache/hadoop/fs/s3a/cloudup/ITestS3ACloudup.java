/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.fs.s3a.cloudup;

import java.io.File;
import java.net.URI;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.contract.AbstractBondedFSContract;
import org.apache.hadoop.fs.contract.AbstractFSContract;
import org.apache.hadoop.fs.contract.AbstractFSContractTestBase;
import org.apache.hadoop.fs.s3a.S3AFileSystem;
import org.apache.hadoop.tools.cloudup.CloudupTestUtils;

import static org.apache.hadoop.fs.contract.ContractTestUtils.cleanup;
import static org.apache.hadoop.tools.cloudup.CloudupTestUtils.*;

/**
 * As the S3A test base isn't available, do enough to make it look
 * like it is, to ease later merge.
 */
public class ITestS3ACloudup extends AbstractFSContractTestBase {
  protected static final Logger LOG =
      LoggerFactory.getLogger(ITestS3ACloudup.class);
  private Path root;
  private Path testPath;

  @Override
  protected AbstractFSContract createContract(Configuration conf) {
    return new S3AContract(conf);
  }

  private static File testDirectory;
  private File methodDir;
  private File sourceDir;
  private S3AFileSystem fileSystem;

  @Rule
  public TestName methodName = new TestName();

  /**
   * Set the timeout for every test.
   */
  @Rule
  public Timeout testTimeout = new Timeout(60 * 1000);

  @BeforeClass
  public static void classSetup() throws Exception {
    Thread.currentThread().setName("JUnit");
    testDirectory = createTestDir();
  }

  public Configuration createConfiguration() {
    return new Configuration();
  }

  @Override
  public S3AFileSystem getFileSystem() {
    return fileSystem;
  }

  @Before
  public void setup() throws Exception {
    String key = String.format(AbstractBondedFSContract.FSNAME_OPTION, "s3a");
    Configuration conf = createConfiguration();
    String fsVal = conf.getTrimmed(key);
    assertFalse("No FS set in " + key, StringUtils.isEmpty(fsVal));
    URI fsURI = new URI(fsVal);
    assertEquals("Not an S3A Filesystem: " + fsURI,
        "s3a", fsURI.getScheme());
    fileSystem = (S3AFileSystem) FileSystem.get(fsURI, conf);
    root = new Path(getFileSystem().getUri());
    testPath = new Path(root, "/ITestS3ACloudup");


    methodDir = new File(testDirectory, methodName.getMethodName());
    CloudupTestUtils.mkdirs(methodDir);
    sourceDir = new File(methodDir, "src");
    FileUtil.fullyDelete(sourceDir);
  }


  @After
  public void teardown() throws Exception {
    if (methodDir != null) {
      FileUtil.fullyDelete(methodDir);
    }
    cleanup("TEARDOWN", getFileSystem(), testPath);
  }

  @Test
  public void testUpload() throws Throwable {
    Path dest = methodPath();
    int expected = createTestFiles(sourceDir, 256);
    expectSuccess(
        "-s", sourceDir.toURI().toString(),
        "-d", dest.toUri().toString(),
        "-t", "16",
        "-o",
        "-l", "3");

  }

  Path methodPath() {
    return new Path(testPath, methodName.getMethodName());
  }

}
