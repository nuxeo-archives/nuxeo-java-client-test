/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *         Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.java.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.java.client.api.objects.Document;
import org.nuxeo.java.client.api.objects.Documents;
import org.nuxeo.java.client.api.objects.Operation;
import org.nuxeo.java.client.api.objects.blob.Blob;
import org.nuxeo.java.client.api.objects.operation.DocRef;
import org.nuxeo.java.client.api.objects.operation.DocRefs;
import org.nuxeo.java.client.internals.spi.NuxeoClientException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.google.common.io.Files;

/**
 * @since 0.1
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@Jetty(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class TestOperation extends TestBase {

    @Before
    public void authentication() {
        login();
    }

    @Test
    public void itCanExecuteOperationOnDocument() {
        Document result = (Document) nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
        assertNotNull(result);
    }

    @Test
    public void itCanExecuteOperationOnDocuments() {
        Operation operation = nuxeoClient.automation("Repository.Query").param("query", "SELECT * " + "FROM Document");
        Documents result = (Documents) operation.execute();
        assertNotNull(result);
        assertTrue(result.getTotalSize() != 0);
    }

    @Test
    public void itCanExecuteOperationOnBlob() throws IOException {
        // Get a blob
        Document result = (Document) nuxeoClient.automation()
                                                .param("value", "/folder_2/file")
                                                .execute("Repository.GetDocument");
        Blob blob = (Blob) nuxeoClient.automation().input(result).execute("Document.GetBlob");
        assertNotNull(blob);
        List<String> lines = Files.readLines(blob.getFile(), Charset.defaultCharset());
        assertEquals("[", lines.get(0));
        assertEquals("    \"fieldType\": \"string\",", lines.get(2));
        // Attach a blob
        File temp1 = File.createTempFile("pattern", ".suffix");
        temp1.deleteOnExit();
        BufferedWriter out1 = new BufferedWriter(new FileWriter(temp1));
        out1.write("1String");
        out1.close();
        Blob fileBlob = new Blob(temp1);
        int length = fileBlob.getLength();
        blob = (Blob) nuxeoClient.automation()
                                 .newRequest("Blob.AttachOnDocument")
                                 .param("document", "/folder_2/file")
                                 .input(fileBlob)
                                 .execute();
        assertNotNull(blob);
        assertEquals(length, blob.getLength());
        Blob resultBlob = (Blob) nuxeoClient.automation().input("/folder_2/file").execute("Document.GetBlob");
        assertNotNull(resultBlob);
        assertEquals(length, resultBlob.getLength());
    }

    @Ignore("TODO")
    @Test
    public void itCanExecuteOperationOnBlobs() throws IOException {
        // Attach a blobs and get them
        File temp1 = File.createTempFile("pattern", ".suffix");
        File temp2 = File.createTempFile("pattern", ".suffix");
        temp1.deleteOnExit();
        temp2.deleteOnExit();
        BufferedWriter out1 = new BufferedWriter(new FileWriter(temp1));
        BufferedWriter out2 = new BufferedWriter(new FileWriter(temp2));
        out1.write("1String");
        out2.write("2String");
        out1.close();
        out2.close();
        Blob fileBlob = new Blob(temp1);
        int length = fileBlob.getLength();
        Blob blob = (Blob) nuxeoClient.automation()
                                      .newRequest("Blob.AttachOnDocument")
                                      .param("document", "/folder_2/file")
                                      .input(fileBlob)
                                      .execute();
        assertNotNull(blob);
        assertEquals(length, blob.getLength());
        // TODO handle multiple parts reading
        List<Blob> resultBlobs = (List<Blob>) nuxeoClient.automation()
                                                         .input("/folder_2" +
                                                                 "/file")
                                                         .execute("Document.GetBlobs");
        assertNotNull(resultBlobs);
        assertEquals(2, resultBlobs.size());
    }

    @Test
    public void testMultiThread() throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                Document result = (Document) nuxeoClient.automation()
                                                        .param("value", "/")
                                                        .execute("Repository.GetDocument");
                assertNotNull(result);
            } catch (Exception e) {
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                Document result = (Document) nuxeoClient.automation()
                                                        .param("value", "/")
                                                        .execute("Repository.GetDocument");
                assertNotNull(result);
            } catch (Exception e) {
            }
        });
        t.start();
        t2.start();
        t.join();
        t2.join();
    }

    @Test
    public void itCanExecuteOperationOnVoid() {
        try {
            nuxeoClient.automation()
                       .newRequest("Log")
                       .param("message", "Error Log Test")
                       .param("level", "error")
                       .execute();
        } catch (NuxeoClientException reason) {
            fail("Void operation failing");
        }
    }

    // FIXME
    @Ignore
    @Test
    public void itCanExecuteOperationWithDocumentRefs() {
        Document result = (Document) nuxeoClient.automation().param("value", "/").execute("Repository.GetDocument");
        assertNotNull(result);
        DocRefs docRefs = new DocRefs();
        docRefs.addDoc(new DocRef(result.getId()));
        result = (Document) nuxeoClient.automation()
                                       .input(docRefs)
                                       .param("properties", null)
                                       .execute("Document.Update");
        assertNotNull(result);
    }
}