package org.apache.wiki.markdown.migration.parser;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.parser.WikiDocument;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.Text;

import java.io.IOException;
import java.io.Reader;
import java.util.List;


public class JSPWikiToMarkdownMarkupParser extends JSPWikiMarkupParser {

    /**
     * Creates a markup parser.
     *
     * @param context The WikiContext which controls the parsing
     * @param in      Where the data is read from.
     */
    public JSPWikiToMarkdownMarkupParser( final Context context, final Reader in ) {
        super( context, in );
    }

    /** {@inheritDoc} */
    @Override
    public WikiDocument parse() throws IOException {
        final WikiDocument doc = super.parse();
        translatePluginACLAndVariableTextLinksToMarkdown( doc.getRootElement(), 0 );
        return doc;
    }

    void translatePluginACLAndVariableTextLinksToMarkdown( final Content element, final int childNumber ) {
        if( element instanceof PluginContent ) {
            final PluginContent plugin = ( PluginContent ) element;
            final String str = plugin.getText();
            if( str.startsWith( "[{" ) && str.endsWith( "}]" ) ) {
                final Element parent = plugin.getParent();
                plugin.detach();
                if( parent != null ) {
                    parent.addContent( childNumber, new Text( str + "()" ) );
                }
            }
        } else if( element instanceof Text ) {
            final Text text = ( Text )element;
            if( text.getText().startsWith( "[{" ) && text.getText().endsWith( "}]" ) ) {
                text.append( "()" );
            }
        } else if( element instanceof Element ) {
            final Element base = ( Element )element;
            base.getContent();
            final List< Content > content = base.getContent();
            for( int i = 0; i < content.size(); i++ ) {
                final Content c = content.get( i );
                translatePluginACLAndVariableTextLinksToMarkdown( c, i );
            }
        }
    }

}
