/*
 * Copyright 2026 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.plugin;

import org.apache.wiki.TestEngine;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class ImageTest {

    static TestEngine testEngine = TestEngine.build();

    /**
     * Test of execute method, of class Image.
     */
    @Test
    public void mixedCase() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘JavaScript:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void withSpaces() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘Java Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void withTabSpaces() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘Java\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void withLeadingSpaces() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘ Java\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void withLeading2Spaces() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘\tJava\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void dataurl() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘\tDATA:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void vbscripturl() throws Exception {
        final String src = "[{Image src=’img.png’ link=‘\tVbs c  ripT:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }
    
    
   
    
    @Test
    public void SRCmixedCase() throws Exception {
        final String src = "[{Image src=‘JavaScript:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"));

    }

    @Test
    public void srcwithSpaces() throws Exception {
        final String src = "[{Image src=‘Java Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void srcwithTabSpaces() throws Exception {
        final String src = "[{Image src=‘Java\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void srcwithLeadingSpaces() throws Exception {
        final String src = "[{Image src=‘ Java\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void srcwithLeading2Spaces() throws Exception {
        final String src = "[{Image src=‘\tJava\t Script:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void SRCdataurl() throws Exception {
        final String src = "[{Image src=‘\tDATA:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

    @Test
    public void SRCvbscripturl() throws Exception {
        final String src = "[{Image src=‘\tVbs c  ripT:alert(document.cookie)>’}]";

        testEngine.saveText("ThisPage", src);

        // Just check that it contains a proper error message; don't bother do HTML checking.
        final String res = testEngine.getManager(RenderingManager.class).getHTML("ThisPage");
        Assertions.assertTrue(!res.contains("document.cookie"), res);

    }

}
