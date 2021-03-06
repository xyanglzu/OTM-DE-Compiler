/**
 * Copyright (C) 2014 OpenTravel Alliance (info@opentravel.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opentravel.schemacompiler.codegen.html.markup;

import org.opentravel.schemacompiler.codegen.html.Content;
import org.opentravel.schemacompiler.codegen.html.DocletAbortException;
import org.opentravel.schemacompiler.codegen.html.DocletConstants;


/**
 * Class for generating a comment for HTML pages of javadoc output.
 *
 * @author Bhavesh Patel
 */
public class Comment extends Content {

    private String commentText;

    /**
     * Constructor to construct a Comment object.
     *
     * @param comment comment text for the comment
     */
    public Comment(String comment) {
        commentText = nullCheck( comment );
    }

    /**
     * This method is not supported by the class.
     *
     * @param content content that needs to be added
     * @throws DocletAbortException this method will always throw a DocletAbortException because it is not supported.
     */
    public void addContent(Content content) {
        throw new DocletAbortException();
    }

    /**
     * This method is not supported by the class.
     *
     * @param stringContent string content that needs to be added
     * @throws DocletAbortException this method will always throw a DocletAbortException because it is not supported.
     */
    public void addContent(String stringContent) {
        throw new DocletAbortException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return commentText.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public void write(StringBuilder contentBuilder) {
        if (!endsWithNewLine( contentBuilder )) {
            contentBuilder.append( DocletConstants.NL );
        }
        contentBuilder.append( "<!-- " );
        contentBuilder.append( commentText );
        contentBuilder.append( " -->" + DocletConstants.NL );
    }
}
