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

package org.opentravel.schemacompiler.codegen.html.writers.info;

import org.opentravel.schemacompiler.codegen.html.Content;
import org.opentravel.schemacompiler.codegen.html.builders.AttributeOwnerDocumentationBuilder;
import org.opentravel.schemacompiler.codegen.html.markup.HtmlConstants;
import org.opentravel.schemacompiler.codegen.html.markup.HtmlStyle;
import org.opentravel.schemacompiler.codegen.html.markup.HtmlTag;
import org.opentravel.schemacompiler.codegen.html.markup.HtmlTree;
import org.opentravel.schemacompiler.codegen.html.writers.SubWriterHolderWriter;

/**
 * @author Eric.Bronson
 *
 */
public class ExampleInfoWriter extends AbstractInfoWriter<AttributeOwnerDocumentationBuilder<?>> {

    /**
     * @param writer the writer for which to create an info-writer
     * @param owner the owner of the new info-writer
     */
    public ExampleInfoWriter(SubWriterHolderWriter writer, AttributeOwnerDocumentationBuilder<?> owner) {
        super( writer, owner );
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.info.InfoWriter#addInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addInfo(Content memberTree) {
        addInfoSummary( memberTree );
    }

    protected void addInfoSummary(Content memberTree) {
        Content exampleTree = getExampleTree();
        addExample( source.getExampleXML(), "XML", exampleTree );
        addExample( source.getExampleJSON(), "JSON", exampleTree );
        memberTree.addContent( exampleTree );
    }


    /**
     * Get the member tree
     *
     * @return a content tree for the member
     */
    public Content getExampleTree() {
        HtmlTree div = new HtmlTree( HtmlTag.DIV );
        div.setStyle( HtmlStyle.EXAMPLE );
        return div;
    }

    protected Content getExampleHeader(String labelKey) {
        Content header = new HtmlTree( HtmlTag.DIV );
        Content label = writer.getResource( ("doclet.Example_" + labelKey) );
        HtmlTree labelHeading = HtmlTree.heading( HtmlConstants.INHERITED_SUMMARY_HEADING, label );
        addCollapseTrigger( labelHeading, "EXAMPLE" + labelKey, labelKey );
        header.addContent( labelHeading );
        return header;
    }

    protected void addExample(String example, String labelKey, Content exampleTree) {
        exampleTree.addContent( getExampleHeader( labelKey ) );
        HtmlTree pre = new HtmlTree( HtmlTag.PRE );
        pre.addContent( example );
        HtmlTree exampleDiv = HtmlTree.div( pre );
        makeCollapsible( exampleDiv, "EXAMPLE" + labelKey );
        exampleTree.addContent( exampleDiv );
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.info.AbstractInfoWriter#getInfoTableSummary()
     */
    @Override
    protected String getInfoTableSummary() {
        return "";
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.info.AbstractInfoWriter#getInfoTableHeader()
     */
    @Override
    protected String[] getInfoTableHeader() {
        return new String[0];
    }

}
