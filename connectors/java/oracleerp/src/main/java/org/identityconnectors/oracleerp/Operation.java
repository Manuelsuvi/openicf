/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (the "License").  You may not use this file
 * except in compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://opensource.org/licenses/cddl1.php
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://opensource.org/licenses/cddl1.php.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import org.identityconnectors.common.Assertions;

/**
 * Main implementation of the Operation.
 *
 * @author petr
 * @since 1.0
 */
abstract class Operation {

    /**
     * The instance or the parent object.
     */
    final private OracleERPConnection conn;

    /**
     * get method.
     *
     * @return the connection
     */
    OracleERPConnection getConn() {
        return conn;
    }

    /**
     * get method.
     *
     * @return the configuration
     */
    OracleERPConfiguration getCfg() {
        return cfg;
    }

    /**
     * The instance or the parent object.
     */
    final private OracleERPConfiguration cfg;

    /**
     * The base of all operation.
     *
     * @param conn
     * @param cfg
     */
    protected Operation(OracleERPConnection conn, OracleERPConfiguration cfg) {
        super();
        Assertions.nullCheck(conn, "connector connection");
        Assertions.nullCheck(cfg, "connector configuration");
        this.conn = conn;
        this.cfg = cfg;
    }
}
