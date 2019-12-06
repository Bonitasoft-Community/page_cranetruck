package com.bonitasoft.custompage.cranetruck;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class LdapLoginModuleCheck {

    static Logger logger = Logger.getLogger(LdapLoginModuleCheck.class.getName());

    public String mUserProvider;
    public String mUserFilter;
    public String mAuthIdentity;
    public boolean mUseSSL;
    public String mLogin;
    public String mPassword;

    static final BEvent eventCalculAuthId = new BEvent(LdapLoginModuleCheck.class.getName(), 1, BEvent.Level.INFO,
            "Authentication Id", "The authentication information is recalculted using the login id");
    static final BEvent eventConnectionSuccessfull = new BEvent(LdapLoginModuleCheck.class.getName(), 2, BEvent.Level.SUCCESS,
            "Connection successfull", "The connection to the LDAP database is correct");
    static final BEvent eventCalculSearchBy = new BEvent(LdapLoginModuleCheck.class.getName(), 3, BEvent.Level.INFO,
            "Search By in the hierarchy", "The search operation is calculated, replaced the name by the login");

    static final BEvent eventSearchBySuccess = new BEvent(LdapLoginModuleCheck.class.getName(), 4, BEvent.Level.SUCCESS,
            "Search with success", "The LDap Search operation run without syntax error or access right");

    static final BEvent eventFoundOneUser = new BEvent(LdapLoginModuleCheck.class.getName(), 5, BEvent.Level.SUCCESS,
            "User found", "One user is correctly found with theses parameter");

    static final BEvent eventSearchNoResult = new BEvent(LdapLoginModuleCheck.class.getName(), 6, BEvent.Level.APPLICATIONERROR,
            "No User found", "No user was found by the search criteria and the login name");

    static final BEvent eventError = new BEvent(LdapLoginModuleCheck.class.getName(), 7, BEvent.Level.APPLICATIONERROR,
            "Error LDAP", "An error arrive during the execution", "Check the message");

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Read parameters method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public static LdapLoginModuleCheck getInstanceFromJsonSt(final String jsonSt) {

        logger.info("LdapLoginModuleCheck.getInstanceFromJsonSt: Receive parametersJson=" + jsonSt);
        final LdapLoginModuleCheck ldapLoginModuleCheck = new LdapLoginModuleCheck();
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            logger.severe("Can't decode jsonSt " + jsonSt);

            return new LdapLoginModuleCheck();
        }
        logger.info("Receive parametersJson=" + jsonSt);
        ldapLoginModuleCheck.mUserProvider = Toolbox.getString(jsonHash.get("userProvider"), null);
        ldapLoginModuleCheck.mUserFilter = Toolbox.getString(jsonHash.get("userFilter"), null);
        ldapLoginModuleCheck.mAuthIdentity = Toolbox.getString(jsonHash.get("authIdentity"), null);
        ldapLoginModuleCheck.mUseSSL = Toolbox.getBoolean(jsonHash.get("useSSL"), false);
        ldapLoginModuleCheck.mLogin = Toolbox.getString(jsonHash.get("login"), null);
        ldapLoginModuleCheck.mPassword = Toolbox.getString(jsonHash.get("password"), null);

        return ldapLoginModuleCheck;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Check method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /***
     * check check the Ldap
     *
     * @param mHostURL
     * @param mAuthType
     * @param mPrincipalDN
     * @param mPassword
     * @param mSearchDN
     * @param mSearchFilter
     * @return
     */
    public StatusOperation checkLdapLoginModuleConnection() {
        final StatusOperation statusOperation = new StatusOperation("ldapLoginModule");

        SearchControls constraints = null;
        final Hashtable<String, Object> ldapEnvironment = new Hashtable<String, Object>(9);
        LdapContext ctx;
        final Pattern USERNAME_PATTERN = Pattern.compile("\\{USERNAME\\}");

        ldapEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnvironment.put(Context.PROVIDER_URL, mUserProvider);
        if (mUseSSL) {
            ldapEnvironment.put(Context.SECURITY_PROTOCOL, "ssl");
        } else {
            ldapEnvironment.remove(Context.SECURITY_PROTOCOL);
        }
        final Matcher identityMatcher = USERNAME_PATTERN.matcher(mAuthIdentity);

        final String id = replaceUsernameToken(identityMatcher, mAuthIdentity, mLogin);
        statusOperation.listEvents.add(new BEvent(eventCalculAuthId, "Match [" + mAuthIdentity + "] with userName["
                + mLogin + "] => Login:[" + id + "]"));

        ldapEnvironment.put(Context.SECURITY_CREDENTIALS, mPassword);
        ldapEnvironment.put(Context.SECURITY_PRINCIPAL, id);

        try {
            // Connect to the LDAP server (using simple bind)
            ctx = new InitialLdapContext(ldapEnvironment, null);
            statusOperation.listEvents.add(eventConnectionSuccessfull);

            constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            constraints.setReturningAttributes(new String[0]); // return no
                                                               // attrs
            constraints.setReturningObjFlag(true); // to get the full DN

            final Matcher filterMatcher = USERNAME_PATTERN.matcher(mUserFilter);
            final String searchBy = replaceUsernameToken(filterMatcher, mUserFilter, mLogin);
            statusOperation.listEvents.add(new BEvent(eventCalculSearchBy, "Match [" + mUserFilter + "] with userName["
                    + mLogin + "] => searchBy:[" + searchBy + "]"));

            final NamingEnumeration<SearchResult> results = ctx.search("", searchBy, constraints);

            statusOperation.listEvents.add(new BEvent(eventSearchBySuccess, "Search success"));

            // Extract the distinguished name of the user's entry
            // (Use the first entry if more than one is returned)
            if (results.hasMore()) {
                final SearchResult entry = results.next();

                // %%% - use the SearchResult.getNameInNamespace method
                // available in JDK 1.5 and later.
                // (can remove call to constraints.setReturningObjFlag)
                final String userDN = ((Context) entry.getObject()).getNameInNamespace();
                statusOperation.listEvents.add(new BEvent(eventFoundOneUser, "found userDn[" + userDN + "]"));

                final StringBuffer theJaasContent = new StringBuffer();
                theJaasContent.append("BonitaAuthentication-1 {<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;com.sun.security.auth.module.LdapLoginModule REQUIRED<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;userProvider=\"" + mUserProvider + "\"<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;userFilter=\"" + mUserFilter + "\"<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;authIdentity=\"" + mAuthIdentity + "\"<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;debug=true<br>");
                theJaasContent.append("&nbsp;&nbsp;&nbsp;useSSL=" + (mUseSSL ? "true" : "false") + "<br>");
                theJaasContent.append(" }");
                statusOperation.addAdditionalInfo("jaasfile", theJaasContent.toString());

            } else {

                statusOperation.listEvents.add(eventSearchNoResult);
                statusOperation.setSuccess("Connection Success");
            }
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();

            statusOperation.listEvents.add(new BEvent(eventError, "Exception " + e.toString() + "<br>" + exceptionDetails));
            statusOperation.addError("Connection failed");
            logger.severe("Exception " + e.toString() + " at " + exceptionDetails);
        }
        return statusOperation;
    }

    /**
     * @param matcher
     * @param string
     * @param userName
     * @return
     */
    private String replaceUsernameToken(final Matcher matcher, final String string, final String userName) {
        return matcher != null ? matcher.replaceAll(userName) : string;
    }
}
