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

package org.opentravel.schemacompiler.codegen.html.writers;

import org.opentravel.schemacompiler.codegen.html.Content;
import org.opentravel.schemacompiler.codegen.html.builders.DocumentationBuilder;
import org.opentravel.schemacompiler.codegen.html.builders.FacetDocumentationBuilder;
import org.opentravel.schemacompiler.codegen.html.writers.info.AliasInfoWriter;
import org.opentravel.schemacompiler.codegen.html.writers.info.ExampleInfoWriter;
import org.opentravel.schemacompiler.codegen.html.writers.info.FacetAttributeInfoWriter;
import org.opentravel.schemacompiler.codegen.html.writers.info.FacetIndicatorInfoWriter;
import org.opentravel.schemacompiler.codegen.html.writers.info.InfoWriter;
import org.opentravel.schemacompiler.codegen.html.writers.info.PropertyInfoWriter;

import java.io.IOException;

/**
 * @author Eric.Bronson
 *
 */
@SuppressWarnings("squid:MaximumInheritanceDepth")
public class FacetWriter extends NamedEntityWriter<FacetDocumentationBuilder>
    implements FieldOwnerWriter, AliasOwnerWriter {

    /**
     * @param classDoc the documentation builder for which to construct a writer
     * @param prev the previous documentation writer
     * @param next the next documentation writer
     * @throws IOException thrown if the writer's output cannot be published
     */
    public FacetWriter(FacetDocumentationBuilder classDoc, DocumentationBuilder prev, DocumentationBuilder next)
        throws IOException {
        super( classDoc, prev, next );
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.AliasOwnerWriter#addAliasInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addAliasInfo(Content aliasTree) {
        if (!member.getAliases().isEmpty()) {
            new AliasInfoWriter( this, member ).addInfo( aliasTree );
        }
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.FieldOwnerWriter#addPropertyInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addPropertyInfo(Content memberTree) {
        if (!member.getProperties().isEmpty()) {
            new PropertyInfoWriter( this, member ).addInfo( memberTree );
        }
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.FieldOwnerWriter#addAttributeInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addAttributeInfo(Content memberTree) {
        if (!member.getAttributes().isEmpty()) {
            new FacetAttributeInfoWriter( this, member ).addInfo( memberTree );
        }
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.FieldOwnerWriter#addIndicatorInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addIndicatorInfo(Content memberTree) {
        if (!member.getIndicators().isEmpty()) {
            new FacetIndicatorInfoWriter( this, member ).addInfo( memberTree );
        }
    }

    /**
     * @see org.opentravel.schemacompiler.codegen.html.writers.FieldOwnerWriter#addExampleInfo(org.opentravel.schemacompiler.codegen.html.Content)
     */
    @Override
    public void addExampleInfo(Content memberTree) {
        InfoWriter exampleWriter = new ExampleInfoWriter( this, member );
        exampleWriter.addInfo( memberTree );
    }
}
