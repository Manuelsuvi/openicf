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
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations
 * under the License.
 *
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.oracleerp;

import static org.identityconnectors.oracleerp.OracleERPUtil.*;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.Assertions;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.dbcommon.FilterWhereBuilder;
import org.identityconnectors.dbcommon.SQLParam;
import org.identityconnectors.dbcommon.SQLUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

/**
 * Main implementation of the OracleErp Connector
 *
 * @author petr
 * @version 1.0
 * @since 1.0
 */
@ConnectorClass(displayNameKey = "oracleerp.connector.display", configurationClass = OracleERPConfiguration.class)
public class OracleERPConnector implements Connector, AuthenticateOp, DeleteOp, SearchOp<FilterWhereBuilder>, UpdateOp,
        CreateOp, TestOp, SchemaOp, ScriptOnConnectorOp, AttributeNormalizer {

    /**
     * Setup logging for the {@link OracleERPConnector}.
     */
    static final Log log = Log.getLog(OracleERPConnector.class);

    /**
     * Place holder for the {@link Configuration} passed into the init() method {@link OracleERPConnector#init}.
     */
    private OracleERPConfiguration cfg;

    /**
     * Gets the Configuration context for this connector.
     * @return connector configuration
     */
    public Configuration getConfiguration() {
        return this.cfg;
    }

    /**
     * getter method
     * @return OracleERPConfiguration
     */
    OracleERPConfiguration getCfg() {
        return cfg;
    }

    /**
     * The OrecleERP connection wrapper
     */
    private OracleERPConnection conn;

    OracleERPConnection getConn() {
        return conn;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.AuthenticateOp#authenticate(java.lang.String, org.identityconnectors.common.security.GuardedString, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid authenticate(ObjectClass objectClass, final String username, final GuardedString password,
            final OperationOptions options) {
        log.info("authenticate");

        Assertions.nullCheck(objectClass, "objectClass");
        Assertions.nullCheck(username, "username");
        Assertions.nullCheck(password, "password");

        if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationAutenticate(getConn(), getCfg()).authenticate(objectClass, username, password, options);
        }
        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, objectClass.toString()));
    }

    /******************
     * SPI Operations
     *
     * Implement the following operations using the contract and description found in the Javadoc for these methods.
     ******************/

    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        log.info("create");
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(attrs, "attrs");
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }

        //doBeforeCreateActionScripts(oclass, attrs, options);

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationCreate(getConn(), getCfg()).create(oclass, attrs, options);
        } else if (oclass.is(RESP_NAMES)) {
            // TODO add create "respNames" function
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));

    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#createFilterTranslator(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public FilterTranslator<FilterWhereBuilder> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        log.info("createFilterTranslator");

        Assertions.nullCheck(oclass, "oclass");

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            return new RespNamesOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            return new ResponsibilitiesOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.DIRECT_RESPS)) {
            return new ResponsibilitiesOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.INDIRECT_RESPS)) {
            return new ResponsibilitiesOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.APPS)) {
            return new ApplicationOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.AUDITOR_RESPS)) {
            return new AuditorOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.SEC_GROUPS)) {
            return new SecuringGroupsOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        } else if (oclass.is(OracleERPUtil.SEC_ATTRS)) {
            return new SecuringAttributesOperationSearch(getConn(), getCfg()).createFilterTranslator(oclass, options);
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.DeleteOp#delete(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Uid, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        log.info("delete");
        Assertions.nullCheck(objClass, "oclass");
        Assertions.nullCheck(uid, "uid");

        if (uid == null || uid.getUidValue() == null) {
            throw new IllegalArgumentException(getCfg().getMessage(MSG_ACCOUNT_UID_REQUIRED));
        }

        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            new AccountOperationDelete(getConn(), getCfg()).delete(objClass, uid, options);
            return;
        } else if (objClass.is(RESP_NAMES)) {
            // TODO ad delete RespNames Function
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, objClass.toString()));
    }

    /**
     * Disposes of the {@link OracleERPConnector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        log.info("dispose");
        if (getConn() != null) {
            getConn().dispose();
        }
        cfg = null;
        conn = null;
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.SearchOp#executeQuery(org.identityconnectors.framework.common.objects.ObjectClass, java.lang.Object, org.identityconnectors.framework.common.objects.ResultsHandler, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public void executeQuery(ObjectClass oclass, FilterWhereBuilder where, ResultsHandler handler,
            OperationOptions options) {
        log.info("executeQuery");

        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(handler, "handler");

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            new AccountOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            new RespNamesOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.RESPS)) {
            new ResponsibilitiesOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.DIRECT_RESPS)) {
            new ResponsibilitiesOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.INDIRECT_RESPS)) {
            new ResponsibilitiesOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.APPS)) {
            new ApplicationOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.AUDITOR_RESPS)) {
            new AuditorOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.SEC_GROUPS)) {
            new SecuringGroupsOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        } else if (oclass.is(OracleERPUtil.SEC_ATTRS)) {
            new SecuringAttributesOperationSearch(getConn(), getCfg()).executeQuery(oclass, where, handler, options);
            return;
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.AttributeNormalizer#normalizeAttribute(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Attribute)
     */
    public Attribute normalizeAttribute(ObjectClass oclass, Attribute attribute) {
        //log.info("normalizeAttribute");
        Assertions.nullCheck(oclass, "oclass");
        Assertions.nullCheck(attribute, "attribute");

        if (oclass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.RESP_NAMES)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.RESPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.DIRECT_RESPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.INDIRECT_RESPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.APPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.AUDITOR_RESPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.SEC_GROUPS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        } else if (oclass.is(OracleERPUtil.SEC_ATTRS)) {
            return new BasicNameResolver().normalizeAttribute(oclass, attribute);
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, oclass.toString()));
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.ScriptOnConnectorOp#runScriptOnConnector(org.identityconnectors.framework.common.objects.ScriptContext, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {
        log.info("runScriptOnConnector");

        Assertions.nullCheck(request, "request");
        Assertions.nullCheck(options, "options");
        return new OracleERPOperationRunScriptOnConnector(getConn(), getCfg()).runScriptOnConnector(request, options);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.identityconnectors.framework.spi.operations.SchemaOp#schema()
     */
    public Schema schema() {
        log.info("schema");
        if (getCfg().getSchema() == null) {
            getCfg().setSchema(new OracleERPOperationSchema(getConn(), getCfg()).schema());
        }
        return getCfg().getSchema();
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.TestOp#test()
     */
    public void test() {
        log.info("test");
        getCfg().validate();
        getConn().test();
        validateAccountsIncluded();
        
        //TODO Validate Get User After script by compiling it
    }

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.operations.UpdateOp#update(org.identityconnectors.framework.common.objects.ObjectClass, java.util.Set, org.identityconnectors.framework.common.objects.OperationOptions)
     */
    public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        log.info("update");
        Assertions.nullCheck(objClass, "oclass");
        Assertions.nullCheck(uid, "uid");
        Assertions.nullCheck(attrs, "replaceAttributes");
        if (attrs.isEmpty()) {
            throw new IllegalArgumentException("Invalid attributes provided to a create operation.");
        }

        if (uid == null || uid.getUidValue() == null) {
            throw new IllegalArgumentException(getCfg().getMessage(MSG_ACCOUNT_UID_REQUIRED));
        }

        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            return new AccountOperationUpdate(getConn(), getCfg()).update(objClass, uid, attrs, options);
        } else if (objClass.is(RESP_NAMES)) {
            // TODO update resp names
        }

        throw new IllegalArgumentException(getCfg().getMessage(MSG_UNKNOWN_OPERATION_TYPE, objClass.toString()));
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init
     */
    public void init(Configuration configuration) {
        /*
         * RA: startConnection():
         * TODO Compile the "get user after" script in init
         *  convert _dbu = new OracleDBUtil(); _dbu.setUpArgs(_resource)
         *  initUserName(), is implemented in OracleERPConfiguration: getSchemaId
         *  _ctx = makeConnection(result);
         */
        log.info("init");
        this.cfg = (OracleERPConfiguration) configuration;
        this.conn = OracleERPConnection.createConnection(getCfg());
        log.ok("createOracleERPConnection");

        getCfg().setUserId(new SecuringAttributesOperations(getConn(), getCfg()).getUserId(getCfg().getUser()));
        log.ok("Init: for user {0} the configUserId is {1}", getCfg().getUser(), getCfg().getUserId());

        initResponsibilities();
        initFndGlobal();
        schema();
        log.info("init done");
    }

    /**
     * Init connector`s call
     */
    private void initFndGlobal() {
        final String respId = getCfg().getRespId();
        final String respApplId = getCfg().getRespApplId();
        log.ok("Init global respId={0}, respApplId={1}", respId, respApplId);
        //Real initialize call
        if (StringUtil.isNotBlank(getCfg().getUserId()) && StringUtil.isNotBlank(respId)
                && StringUtil.isNotBlank(respApplId)) {
            CallableStatement cs = null;
            try {
                final String sql = "call " + getCfg().app() + "FND_GLOBAL.APPS_INITIALIZE(?,?,?)";
                final String msg = "Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE({1}, {2}, {3}) called.";
                log.ok(msg, getCfg().app(), getCfg().getUserId(), respId, respApplId);
                List<SQLParam> pars = new ArrayList<SQLParam>();
                pars.add(new SQLParam("userId", getCfg().getUserId(), Types.VARCHAR));
                pars.add(new SQLParam("respId", respId, Types.VARCHAR));
                pars.add(new SQLParam("respAppId", respApplId, Types.VARCHAR));

                cs = getConn().prepareCall(sql, pars);
                cs.execute();
                // Result ?
                // cstmt1 closed in finally below

            } catch (SQLException e) {
                final String msg = "Oracle ERP: Failed to call {0}FND_GLOBAL.APPS_INITIALIZE()";
                log.error(e, msg, getCfg().app());
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(cs);
                cs = null;
            }
        } else {
            log.info("Oracle ERP: {0}FND_GLOBAL.APPS_INITIALIZE() NOT called.", getCfg().app());
        }
    }

    /**
     * Init the responsibilities
     *
     * @param configUserId
     *            configUserId
     */
    private void initResponsibilities() {
        log.info("initResponsibilities");
        getCfg().setNewResponsibilityViews(getNewResponsibilityViews());

        if (getCfg().isNewResponsibilityViews()) {
            getCfg().setDescrExists(getDescriptionExiests());
        }

        // three pieces of data need for apps_initialize()
        final String auditResponsibility = getCfg().getAuditResponsibility();
        log.ok("auditResponsibility = {0}", auditResponsibility);

        if (StringUtil.isNotBlank(auditResponsibility) && StringUtil.isNotBlank(getCfg().getUserId())) {
            final String view = getCfg().app()
                    + ((getCfg().isNewResponsibilityViews()) ? OracleERPUtil.RESPS_ALL_VIEW : OracleERPUtil.RESPS_TABLE);
            final String sql = "select responsibility_id, responsibility_application_id from "
                    + view
                    + " where user_id = ? and "
                    + "(responsibility_id,responsibility_application_id) = (select responsibility_id,application_id from "
                    + getCfg().app() + "fnd_responsibility_vl where responsibility_name = ?)";

            final String msg = "Oracle ERP SQL: RESP_ID = {1}, RESP_APPL_ID = {2}";

            ArrayList<SQLParam> params = new ArrayList<SQLParam>();
            params.add(new SQLParam("userId", getCfg().getUserId()));
            params.add(new SQLParam("responsibilityName", auditResponsibility));
            PreparedStatement ps = null;
            ResultSet rs = null;
            try {
                log.ok("Select responsibility for user_id: {0}, and audit responsibility {1}", getCfg().getUserId(),
                        auditResponsibility);
                ps = getConn().prepareStatement(sql, params);
                rs = ps.executeQuery();
                if (rs != null) {
                    if (rs.next()) {
                        getCfg().setRespId(rs.getString(1));
                        getCfg().setRespApplId(rs.getString(2));
                    }
                }

                log.info(msg, getCfg().getRespId(), getCfg().getRespApplId());
            } catch (SQLException e) {
                SQLUtil.rollbackQuietly(getConn());
                log.error(e, msg, getCfg().getRespId(), getCfg().getRespApplId());
            } finally {
                // close everything in case we had an exception in the middle of something
                SQLUtil.closeQuietly(rs);
                rs = null;
                SQLUtil.closeQuietly(ps);
                ps = null;
            }
        }
        log.info("initResponsibilities done");
    }

    /**
     * @return the boolean value
     */
    private boolean getDescriptionExiests() {
        final String sql = "select user_id, description from " + getCfg().app()
                + "fnd_user_resp_groups_direct where USER_ID = '9999999999'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = getConn().prepareStatement(sql);
            res = ps.executeQuery();
            log.info("description exists");
            return true;
        } catch (SQLException e) {
            //log.error(e, sql);
            log.info("description does not exists");
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        return false;
    }
    
    /**
     * We double check that the AccountsIncluded clause is valid
     */
    private void validateAccountsIncluded() {
        final String sql = "SELECT 1 FROM "+ getCfg().app() + "fnd_user";
        String testSql = OracleERPUtil.whereAnd(sql, getCfg().getAccountsIncluded());
        PreparedStatement ps = null;
        ResultSet res = null;        
        try {
            ps = getConn().prepareStatement(testSql);
            res = ps.executeQuery();
            log.info("accountsIncluded are ok");
        } catch (SQLException e) {
            log.error(e, testSql);
            throw new IllegalArgumentException(getCfg().getMessage(MSG_INVALID_ACCOUNT_INCLUDED, getCfg().getAccountsIncluded()));
        } finally {
            SQLUtil.closeQuietly(res);
            SQLUtil.closeQuietly(ps);
        }
    }    

    /**
     * The New responsibility format there
     *
     * @return true/false
     */
    private boolean getNewResponsibilityViews() {
        final String sql = "select * from " + getCfg().app()
                + "fnd_views where VIEW_NAME = 'FND_USER_RESP_GROUPS_DIRECT' and APPLICATION_ID = '0'";
        PreparedStatement ps = null;
        ResultSet res = null;
        try {
            ps = getConn().prepareStatement(sql);
            res = ps.executeQuery();
            if (res != null && res.next()) {
                log.info("newResponsibilityViews: true");
                return true;
            }
        } catch (SQLException e) {
            log.error(e, sql);
            SQLUtil.rollbackQuietly(getConn());
            throw ConnectorException.wrap(e);
        } finally {
            SQLUtil.closeQuietly(res);
            res = null;
            SQLUtil.closeQuietly(ps);
            ps = null;
        }
        log.info("newResponsibilityViews: false");
        return false;
    }

}