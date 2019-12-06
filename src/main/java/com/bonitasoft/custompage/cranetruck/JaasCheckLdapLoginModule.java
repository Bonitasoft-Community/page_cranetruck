package com.bonitasoft.custompage.cranetruck;


import java.security.AccessController;
import java.net.SocketPermission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import javax.security.auth.*;
import javax.security.auth.callback.*;
import javax.security.auth.login.*;
import javax.security.auth.spi.*;

import com.sun.security.auth.LdapPrincipal;
import com.sun.security.auth.UserPrincipal;


/** source is 
 * https://github.com/frohoff/jdk8u-dev-jdk/blob/master/src/share/classes/com/sun/security/auth/module/LdapLoginModule.java
 *
 */
public class JaasCheckLdapLoginModule {
    // Use the default classloader for this class to load the prompt strings.
    private static final ResourceBundle rb = AccessController.doPrivileged(
            new PrivilegedAction<ResourceBundle>() {
                public ResourceBundle run() {
                    return ResourceBundle.getBundle(
                        "sun.security.util.AuthResources");
                }
            }
        );

    // Keys to retrieve the stored username and password
    private static final String USERNAME_KEY = "javax.security.auth.login.name";
    private static final String PASSWORD_KEY =
        "javax.security.auth.login.password";

    // Option names
    private static final String USER_PROVIDER = "userProvider";
    private static final String USER_FILTER = "userFilter";
    private static final String AUTHC_IDENTITY = "authIdentity";
    private static final String AUTHZ_IDENTITY = "authzIdentity";

    // Used for the username token replacement
    private static final String USERNAME_TOKEN = "{USERNAME}";
    private static final Pattern USERNAME_PATTERN =
        Pattern.compile("\\{USERNAME\\}");

    // Configurable options
    private String userProvider;
    private String userFilter;
    private String authcIdentity;
    private String authzIdentity;
    private String authzIdentityAttr = null;
    private boolean useSSL = true;
    private boolean authFirst = false;
    private boolean authOnly = false;
    private boolean useFirstPass = false;
    private boolean tryFirstPass = false;
    private boolean storePass = false;
    private boolean clearPass = false;
    private boolean debug = false;

    // Authentication status
    private boolean succeeded = false;
    private boolean commitSucceeded = false;

    // Supplied username and password
    private String username;
    private char[] password;

    // User's identities
    private LdapPrincipal ldapPrincipal;
    private UserPrincipal userPrincipal;
    private UserPrincipal authzPrincipal;

    // Initial state
    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, Object> sharedState;
    private Map<String, ?> options;
    private LdapContext ctx;
    private Matcher identityMatcher = null;
    private Matcher filterMatcher = null;
    private Hashtable<String, Object> ldapEnvironment;
    private SearchControls constraints = null;

    
    public List<String> advancement = new ArrayList<String>();
    /**
     * Initialize this <code>LoginModule</code>.
     *
     * @param subject the <code>Subject</code> to be authenticated.
     * @param callbackHandler a <code>CallbackHandler</code> to acquire the
     *                  username and password.
     * @param sharedState shared <code>LoginModule</code> state.
     * @param options options specified in the login
     *                  <code>Configuration</code> for this particular
     *                  <code>LoginModule</code>.
     */
    // Unchecked warning from (Map<String, Object>)sharedState is safe
    // since javax.security.auth.login.LoginContext passes a raw HashMap.
    @SuppressWarnings("unchecked")
    public void initialize(Subject subject, CallbackHandler callbackHandler,
                        Map<String, ?> sharedState, Map<String, ?> options) {

        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.sharedState = (Map<String, Object>)sharedState;
        this.options = options;

        ldapEnvironment = new Hashtable<String, Object>(9);
        ldapEnvironment.put(Context.INITIAL_CONTEXT_FACTORY,
            "com.sun.jndi.ldap.LdapCtxFactory");

        // Add any JNDI properties to the environment
        for (String key : options.keySet()) {
            if (key.indexOf(".") > -1) {
                ldapEnvironment.put(key, options.get(key));
            }
        }

        // initialize any configured options

        userProvider = (String)options.get(USER_PROVIDER);
        if (userProvider != null) {
            ldapEnvironment.put(Context.PROVIDER_URL, userProvider);
        }

        authcIdentity = (String)options.get(AUTHC_IDENTITY);
        if (authcIdentity != null &&
            (authcIdentity.indexOf(USERNAME_TOKEN) != -1)) {
            identityMatcher = USERNAME_PATTERN.matcher(authcIdentity);
        }

        userFilter = (String)options.get(USER_FILTER);
        if (userFilter != null) {
            if (userFilter.indexOf(USERNAME_TOKEN) != -1) {
                filterMatcher = USERNAME_PATTERN.matcher(userFilter);
            }
            constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            constraints.setReturningAttributes(new String[0]); //return no attrs
            constraints.setReturningObjFlag(true); // to get the full DN
        }

        authzIdentity = (String)options.get(AUTHZ_IDENTITY);
        if (authzIdentity != null &&
            authzIdentity.startsWith("{") && authzIdentity.endsWith("}")) {
            if (constraints != null) {
                authzIdentityAttr =
                    authzIdentity.substring(1, authzIdentity.length() - 1);
                constraints.setReturningAttributes(
                    new String[]{authzIdentityAttr});
            }
            authzIdentity = null; // set later, from the specified attribute
        }

        // determine mode
        if (authcIdentity != null) {
            if (userFilter != null) {
                authFirst = true; // authentication-first mode
            } else {
                authOnly = true; // authentication-only mode
            }
        }

        if ("false".equalsIgnoreCase((String)options.get("useSSL"))) {
            useSSL = false;
            ldapEnvironment.remove(Context.SECURITY_PROTOCOL);
        } else {
            ldapEnvironment.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        tryFirstPass =
                "true".equalsIgnoreCase((String)options.get("tryFirstPass"));

        useFirstPass =
                "true".equalsIgnoreCase((String)options.get("useFirstPass"));

        storePass = "true".equalsIgnoreCase((String)options.get("storePass"));

        clearPass = "true".equalsIgnoreCase((String)options.get("clearPass"));

        // debug = "true".equalsIgnoreCase((String)options.get("debug"));
        
        debug=true;
        
        if (debug) {
            if (authFirst) {

                advancement.add("[LdapLoginModule] " +
                    "authentication-first mode; " +
                    (useSSL ? "SSL enabled" : "SSL disabled"));
            } else if (authOnly) {
                advancement.add("[LdapLoginModule] " +
                    "authentication-only mode; " +
                    (useSSL ? "SSL enabled" : "SSL disabled"));
            } else {
                advancement.add("[LdapLoginModule] " +
                    "search-first mode; " +
                    (useSSL ? "SSL enabled" : "SSL disabled"));
            }
        }
        advancement.add("[LdapLoginModule] initialize Done ....");
    }

    /**
     * Begin user authentication.
     *
     * <p> Acquire the user's credentials and verify them against the
     * specified LDAP directory.
     *
     * @return true always, since this <code>LoginModule</code>
     *          should not be ignored.
     * @exception FailedLoginException if the authentication fails.
     * @exception LoginException if this <code>LoginModule</code>
     *          is unable to perform the authentication.
     */
    public boolean login() throws LoginException {

        if (userProvider == null) {
            throw new LoginException
                ("Unable to locate the LDAP directory service");
        }

        if (debug) {
            advancement.add("[LdapLoginModule] user provider: " +
                userProvider);
        }

        // attempt the authentication
        if (tryFirstPass) {
            advancement.add("[LdapLoginModule] Attempt tryFirstPass");

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (debug) {
                    advancement.add("[LdapLoginModule] " + "tryFirstPass succeeded");
                }
                return true;

            } catch (LoginException le) {
                // authentication failed -- try again below by prompting
                cleanState();
                if (debug) {
                    advancement.add("[LdapLoginModule] " + "tryFirstPass failed: " + le.toString());
                }
            }

        } else if (useFirstPass) {
            advancement.add("[LdapLoginModule] Attempt useFirstPass="+tryFirstPass);

            try {
                // attempt the authentication by getting the
                // username and password from shared state
                attemptAuthentication(true);

                // authentication succeeded
                succeeded = true;
                if (debug) {
                    advancement.add("[LdapLoginModule] " +
                                "useFirstPass succeeded");
                }
                return true;

            } catch (LoginException le) {
                // authentication failed
                cleanState();
                if (debug) {
                    advancement.add("[LdapLoginModule] " +
                                "useFirstPass failed");
                }
                throw le;
            }
        }

        // attempt the authentication by prompting for the username and pwd
        try {

            attemptAuthentication(false);

            // authentication succeeded
           succeeded = true;
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                                "authentication succeeded");
            }
            return true;

        } catch (LoginException le) {
            cleanState();
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                                "authentication failed");
            }
            throw le;
        }
    }

    /**
     * Complete user authentication.
     *
     * <p> This method is called if the LoginContext's
     * overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * succeeded).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates an
     * <code>LdapPrincipal</code> and one or more <code>UserPrincipal</code>s
     * with the <code>Subject</code> located in the
     * <code>LoginModule</code>.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @exception LoginException if the commit fails
     * @return true if this LoginModule's own login and commit
     *          attempts succeeded, or false otherwise.
     */
    public boolean commit() throws LoginException {

        if (succeeded == false) {
            return false;
        } else {
            if (subject.isReadOnly()) {
                cleanState();
                throw new LoginException ("Subject is read-only");
            }
            // add Principals to the Subject
            Set<Principal> principals = subject.getPrincipals();
            if (! principals.contains(ldapPrincipal)) {
                principals.add(ldapPrincipal);
            }
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                                   "added LdapPrincipal \"" +
                                   ldapPrincipal +
                                   "\" to Subject");
            }

            if (! principals.contains(userPrincipal)) {
                principals.add(userPrincipal);
            }
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                                   "added UserPrincipal \"" +
                                   userPrincipal +
                                   "\" to Subject");
            }

            if (authzPrincipal != null &&
                (! principals.contains(authzPrincipal))) {
                principals.add(authzPrincipal);

                if (debug) {
                    advancement.add("[LdapLoginModule] " +
                                   "added UserPrincipal \"" +
                                   authzPrincipal +
                                   "\" to Subject");
                }
            }
        }
        // in any case, clean out state
        cleanState();
        commitSucceeded = true;
        return true;
    }

    /**
     * Abort user authentication.
     *
     * <p> This method is called if the overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules
     * did not succeed).
     *
     * <p> If this LoginModule's own authentication attempt
     * succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods),
     * then this method cleans up any state that was originally saved.
     *
     * @exception LoginException if the abort fails.
     * @return false if this LoginModule's own login and/or commit attempts
     *          failed, and true otherwise.
     */
    public boolean abort() throws LoginException {
        if (debug)
            advancement.add("[LdapLoginModule] " +
                "aborted authentication");

        if (succeeded == false) {
            return false;
        } else if (succeeded == true && commitSucceeded == false) {

            // Clean out state
            succeeded = false;
            cleanState();

            ldapPrincipal = null;
            userPrincipal = null;
            authzPrincipal = null;
        } else {
            // overall authentication succeeded and commit succeeded,
            // but someone else's commit failed
            logout();
        }
        return true;
    }

    /**
     * Logout a user.
     *
     * <p> This method removes the Principals
     * that were added by the <code>commit</code> method.
     *
     * @exception LoginException if the logout fails.
     * @return true in all cases since this <code>LoginModule</code>
     *          should not be ignored.
     */
    public boolean logout() throws LoginException {
        if (subject.isReadOnly()) {
            cleanState();
            throw new LoginException ("Subject is read-only");
        }
        Set<Principal> principals = subject.getPrincipals();
        principals.remove(ldapPrincipal);
        principals.remove(userPrincipal);
        if (authzIdentity != null) {
            principals.remove(authzPrincipal);
        }

        // clean out state
        cleanState();
        succeeded = false;
        commitSucceeded = false;

        ldapPrincipal = null;
        userPrincipal = null;
        authzPrincipal = null;

        if (debug) {
            advancement.add("[LdapLoginModule] logged out Subject");
        }
        return true;
    }

    /**
     * Attempt authentication
     *
     * @param getPasswdFromSharedState boolean that tells this method whether
     *          to retrieve the password from the sharedState.
     * @exception LoginException if the authentication attempt fails.
     */
    private void attemptAuthentication(boolean getPasswdFromSharedState)
        throws LoginException {
        advancement.add("[LdapLoginModule] attemptAuthentication ");
        // first get the username and password
        getUsernamePassword(getPasswdFromSharedState);

        if (password == null || password.length == 0) {
            throw (LoginException)
                new FailedLoginException("No password was supplied");
        }

        String dn = "";
        advancement.add("[LdapLoginModule] authFirst="+authFirst+" authOnly="+authOnly);
        if (authFirst || authOnly) {

            String id = replaceUsernameToken(identityMatcher, authcIdentity);

            // Prepare to bind using user's username and password
            ldapEnvironment.put(Context.SECURITY_CREDENTIALS, password);
            ldapEnvironment.put(Context.SECURITY_PRINCIPAL, id);

            if (debug) {
                advancement.add("[LdapLoginModule] " +
                    "attempting to authenticate user: " + username);
            }
            String log="[LdapLoginModule] InitialLdapContext with SECURITY_PRINCIPAL=["+id+"] SECURITY_CREDENTIALS=["+password+"]... ";
            try {
                // Connect to the LDAP server (using simple bind)
                ctx = new InitialLdapContext(ldapEnvironment, null);
                advancement.add(log+"SUCCESS");

            } catch (NamingException e) {
                advancement.add(log+"FAIL :"+e.getMessage() );
                throw (LoginException)
                    new FailedLoginException("Cannot bind to LDAP server")
                        .initCause(e);
            }

            // Authentication has succeeded

            // Locate the user's distinguished name
            advancement.add("[LdapLoginModule] userFilter==null?"+(userFilter==null ));

            if (userFilter != null) {
                dn = findUserDN(ctx);
            } else {
                dn = id;
            }

        } else {
            String log="[LdapLoginModule] InitialLdapContext without SECURITY_PRINCIPAL and SECURITY_CREDENTIALS ldapEnvironment["+ldapEnvironment.toString()+"]... ";
            
            try {
                // Connect to the LDAP server (using anonymous bind)
                ctx = new InitialLdapContext(ldapEnvironment, null);
                advancement.add(log+"SUCCESS");

            } catch (NamingException e) {
                advancement.add(log+"FAIL :"+e.getMessage() );
                throw (LoginException)
                    new FailedLoginException("Cannot connect to LDAP server")
                        .initCause(e);
            }

            // Locate the user's distinguished name
            advancement.add("[LdapLoginModule] findUser");

            dn = findUserDN(ctx);

            try {

                // Prepare to bind using user's distinguished name and password
                ctx.addToEnvironment(Context.SECURITY_AUTHENTICATION, "simple");
                ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
                ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);

                log="[LdapLoginModule] " + "attempting to authenticate user:[" + username+"]... ";
                
                // Connect to the LDAP server (using simple bind)
                ctx.reconnect(null);
                advancement.add(log+"SUCCESS");

                // Authentication has succeeded

            } catch (NamingException e) {
                advancement.add(log+"FAIL "+e.getMessage());
                throw (LoginException)
                    new FailedLoginException("Cannot bind to LDAP server")
                        .initCause(e);
            }
        }

        // Save input as shared state only if authentication succeeded
        if (storePass &&
            !sharedState.containsKey(USERNAME_KEY) &&
            !sharedState.containsKey(PASSWORD_KEY)) {
            sharedState.put(USERNAME_KEY, username);
            sharedState.put(PASSWORD_KEY, password);
        }

        // Create the user principals
        userPrincipal = new UserPrincipal(username);
        if (authzIdentity != null) {
            authzPrincipal = new UserPrincipal(authzIdentity);
        }

        try {

            ldapPrincipal = new LdapPrincipal(dn);

        } catch (InvalidNameException e) {
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                                   "cannot create LdapPrincipal: bad DN");
            }
            throw (LoginException)
                new FailedLoginException("Cannot create LdapPrincipal")
                    .initCause(e);
        }
        advancement.add("[LdapLoginModule] ... SUCCESS");
        
    }

    /**
     * Search for the user's entry.
     * Determine the distinguished name of the user's entry and optionally
     * an authorization identity for the user.
     *
     * @param ctx an LDAP context to use for the search
     * @return the user's distinguished name or an empty string if none
     *         was found.
     * @exception LoginException if the user's entry cannot be found.
     */
    private String findUserDN(LdapContext ctx) throws LoginException {
        advancement.add("[LdapLoginModule] FindUserDn - start");
        String userDN = "";

        // Locate the user's LDAP entry
        if (userFilter != null) {
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                    "searching for entry belonging to user:[" + username+"]");
            }
        } else {
            if (debug) {
                advancement.add("[LdapLoginModule] " +
                    "cannot search for entry belonging to user:[" + username+"] because userFilter is NULL (a userFilter must be set)");
            }
            throw (LoginException)
                new FailedLoginException("Cannot find user's LDAP entry");
        }

        String userFilterToken= replaceUsernameToken(filterMatcher, userFilter);
        String log="[LdapLoginModule] Search userFilter with ["+userFilterToken+"]...";
        try {
            NamingEnumeration<SearchResult> results = ctx.search("",userFilterToken,constraints);
            advancement.add(log+"SUCCESS, find entry:");
            // Extract the distinguished name of the user's entry
            // (Use the first entry if more than one is returned)
            if (results.hasMore()) {
                SearchResult entry = results.next();

                // %%% - use the SearchResult.getNameInNamespace method
                //        available in JDK 1.5 and later.
                //        (can remove call to constraints.setReturningObjFlag)
                userDN = ((Context)entry.getObject()).getNameInNamespace();

                if (debug) {
                    advancement.add("[LdapLoginModule] found entry: [" +userDN+"]");
                }

                // Extract a value from user's authorization identity attribute
                if (authzIdentityAttr != null) {
                    Attribute attr =
                        entry.getAttributes().get(authzIdentityAttr);
                    if (attr != null) {
                        Object val = attr.get();
                        if (val instanceof String) {
                            authzIdentity = (String) val;
                            advancement.add("[LdapLoginModule] Found authzIdentity["+authzIdentity+"]");
                        }
                    }
                }

                results.close();

            } else {
                // Bad username
                if (debug) {
                    advancement.add("[LdapLoginModule] user's entry ["+userFilterToken+"] not found");
                }
            }

        } catch (NamingException e) {
            // ignore
        }

        if (userDN.equals("")) {
            advancement.add(log+"FAILED, noUserDn found");
            throw (LoginException)
                new FailedLoginException("Cannot find user's LDAP entry");
        } else {
            return userDN;
        }
    }

    /**
     * Replace the username token
     *
     * @param string the target string
     * @return the modified string
     */
    private String replaceUsernameToken(Matcher matcher, String string) {
        return matcher != null ? matcher.replaceAll(username) : string;
    }

    public void setLoginPassword( String loginName, String password)
    {
        this.username = loginName;
        this.password = password.toCharArray();
    }
    /**
     * Get the username and password.
     * This method does not return any value.
     * Instead, it sets global name and password variables.
     *
     * <p> Also note that this method will set the username and password
     * values in the shared state in case subsequent LoginModules
     * want to use them via use/tryFirstPass.
     *
     * @param getPasswdFromSharedState boolean that tells this method whether
     *          to retrieve the password from the sharedState.
     * @exception LoginException if the username/password cannot be acquired.
     */
    private void getUsernamePassword(boolean getPasswdFromSharedState)
        throws LoginException {
        return; // username and password are given 

    }

    /**
     * Clean out state because of a failed authentication attempt
     */
    private void cleanState() {
        username = null;
        if (password != null) {
            Arrays.fill(password, ' ');
            password = null;
        }
        try {
            if (ctx != null) {
                ctx.close();
            }
        } catch (NamingException e) {
            // ignore
        }
        ctx = null;

        if (clearPass) {
            sharedState.remove(USERNAME_KEY);
            sharedState.remove(PASSWORD_KEY);
        }
    }
}