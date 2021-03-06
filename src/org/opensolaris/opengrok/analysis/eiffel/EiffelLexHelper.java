/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License (the "License").
 * You may not use this file except in compliance with the License.
 *
 * See LICENSE.txt included in this distribution for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at LICENSE.txt.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */

/*
 * Copyright (c) 2017, Chris Fraire <cfraire@me.com>.
 */

package org.opensolaris.opengrok.analysis.eiffel;

import java.io.IOException;
import org.opensolaris.opengrok.analysis.Resettable;
import org.opensolaris.opengrok.analysis.JFlexJointLexer;
import org.opensolaris.opengrok.web.HtmlConsts;

/**
 * Represents an API for object's using {@link EiffelLexHelper}.
 */
interface EiffelLexer extends JFlexJointLexer {
}

/**
 * Represents a helper for Eiffel lexers.
 */
class EiffelLexHelper implements Resettable {

    private final EiffelLexer lexer;

    private final int VSTRING;

    /**
     * When matching a Verbatim_string, the expected closer is stored here.
     */
    private String vstring_closer;

    public EiffelLexHelper(int vSTRING, EiffelLexer lexer) {
        if (lexer == null) {
            throw new IllegalArgumentException("`lexer' is null");
        }
        this.lexer = lexer;
        this.VSTRING = vSTRING;
    }

    /**
     * Resets the instance to an initial state.
     */
    @Override
    public void reset() {
        vstring_closer = null;
    }

    /**
     * Begin a "Verbatim_string" according to the specified for the specified
     * "Verbatim_string_opener", {@code opener}, determining the matching
     * "Verbatim_string_closer" to be used with
     * {@link #maybeEndVerbatim(java.lang.String)}.
     * <p>
     * Everything between the first and last character of {@code opener} is the
     * verbatim string α, and the last character's opposite becomes the initial
     * character of the closer.
     * @param opener a defined opener following Eiffel "8.29.10 Syntax:
     * Manifest strings"
     */
    public void vop(String opener) throws IOException {
        int lastoff = opener.length() - 1;
        String alpha = opener.substring(1, lastoff);
        String close0;
        switch (opener.charAt(lastoff)) {
            case '{':
                close0 = "}";
                break;
            case '[':
                close0 = "]";
                break;
            default:
                throw new IllegalArgumentException("Bad opener: " + opener);
        }
        vstring_closer = close0 + alpha + "\"";

        lexer.yypush(VSTRING);
        lexer.disjointSpan(HtmlConsts.STRING_CLASS);
        lexer.offerNonword(opener);
    }

    /**
     * Determine if the specified {@code capture} starts with the expected
     * "Verbatim_string_closer" from
     * {@link #vop(java.lang.String) }, and if so, pop the state, write the
     * closer, and push back any excess; otherwise, write the first character
     * of {@code capture}, and push back the rest.
     * @param capture a defined, possible ender
     * @throws IOException if output fails
     */
    public void maybeEndVerbatim(String capture) throws IOException {
        int npushback;
        if (!capture.startsWith(vstring_closer)) {
            // Nope--so just write the double quote, and push back the rest.
            lexer.offerNonword(capture.substring(0, 1));
            npushback = capture.length() - 1;
        } else {
            lexer.offerNonword(vstring_closer);
            lexer.disjointSpan(null);
            lexer.yypop();
            npushback = capture.length() - vstring_closer.length();
            vstring_closer = null;
        }
        if (npushback > 0) lexer.yypushback(npushback);
    }
}
