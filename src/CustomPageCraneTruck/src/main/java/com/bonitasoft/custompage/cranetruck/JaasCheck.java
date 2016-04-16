package com.bonitasoft.custompage.cranetruck;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.bonitasoft.log.event.BEvent;
import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class JaasCheck {

    static Logger logger = Logger.getLogger(JaasCheck.class.getName());

    static final BEvent eventEnvironnementError = new BEvent(JaasCheck.class.getName(), 1, BEvent.Level.ERROR,
            "No file Error", "The JAAS file can't be read", "Check the path");
    static final BEvent eventNoJaasEnvironnement = new BEvent(JaasCheck.class.getName(), 2, BEvent.Level.ERROR,
            "No Jaas Environnement", "The JAAS Mechanism need to access a Java Environment", "Check the setenv.sh (Tomcat)");

    static final BEvent eventNoJaasConfirgurationFound = new BEvent(JaasCheck.class.getName(), 3, BEvent.Level.ERROR,
            "No Jaas File found", "The Environment Variable give a file name. No file exist", "Check the path");

    static final BEvent eventBadJaasStructure = new BEvent(JaasCheck.class.getName(), 4, BEvent.Level.ERROR,
            "Bad Jaas Structure", "The JAAS file has a structure : it supposed to be KEY { source;source; }, and the brace is not detected",
            "Check the structure");

    private static final String cstParamJaasFile = "jaasfile";
    private static final String cstParamJaasAuthKey = "jaasauthkey";
    private static final String cstParamJaasUserName = "jaasusername";
    private static final String cstParamJaasPassword = "jaaspassword";

    private static final String cstParamJaasOpeUserProvider = "jaasopuserprovider";
    private static final String cstParamJaasOpeUserFilter = "jaasopuserfilter";
    private static final String cstParamJaasOpeUseSSL = "jaasopusessl";
    private static final String cstParamJaasOpIdentity = "jaasopidentity";
    private static final String cstParamJaasOpUserName = "jaasopusername";
    private static final String cstParamJaasOpPassword = "jaasoppassword";

    private String mJaasContent;
    private String mJaasFile;
    private String mJaasAuthentKey;
    private String mJaasUserName;
    private String mJaasPassWord;
    private boolean mShowPassword;

    private String mJaasOpUserProvider;
    private String mJaasOpUserFilter;
    private boolean mJaasOpUseSSL;
    private String mJaasOpIdentity;
    private String mJaasOpUserName;
    private String mJaasOpPassword;

    public class BonitaAuthenticationCallbackHandler implements CallbackHandler {

        private final String name;
        private final String password;

        public BonitaAuthenticationCallbackHandler(final String name, final String password) {
            this.name = name;
            this.password = password;
        }

        public void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (final Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    final NameCallback nc = (NameCallback) callback;
                    nc.setName(name);
                } else if (callback instanceof PasswordCallback) {
                    final PasswordCallback pc = (PasswordCallback) callback;
                    pc.setPassword(password.toCharArray());
                }
            }
        }
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Read parameters method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public static JaasCheck getInstanceFromJsonSt(final String jsonSt)
    {
        logger.info("Receive parametersJson=" + jsonSt);
        final JaasCheck jaasCheck = new JaasCheck();
        final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            logger.severe("Can't decode jsonSt " + jsonSt);

            return new JaasCheck();
        }
        jaasCheck.mJaasContent = Toolbox.getString(jsonHash.get("jaascontent"), null);
        jaasCheck.mJaasFile = Toolbox.getString(jsonHash.get("jaasfile"), null);
        jaasCheck.mJaasAuthentKey = Toolbox.getString(jsonHash.get("jaasauthentkey"), null);
        jaasCheck.mJaasUserName = Toolbox.getString(jsonHash.get("jaasusername"), null);
        jaasCheck.mJaasPassWord = Toolbox.getString(jsonHash.get("jaaspassword"), null);
        jaasCheck.mShowPassword = Toolbox.getBoolean(jsonHash.get("showpassword"), Boolean.FALSE);
        jaasCheck.mJaasOpUserProvider = Toolbox.getString(jsonHash.get("jaasuserprovider"), null);
        jaasCheck.mJaasOpUserFilter = Toolbox.getString(jsonHash.get("jaasuserfilter"), null);
        jaasCheck.mJaasOpUseSSL = Toolbox.getBoolean(jsonHash.get("jaasuseSSL"), Boolean.FALSE);
        jaasCheck.mJaasOpIdentity = Toolbox.getString(jsonHash.get("jaasidentity"), null);
        jaasCheck.mJaasOpUserName = Toolbox.getString(jsonHash.get("jaasopusername"), null);
        jaasCheck.mJaasOpPassword = Toolbox.getString(jsonHash.get("jaasoppassword"), null);

        return jaasCheck;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Check method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    private final static String JAAS_ENV_VARIABLE = "java.security.auth.login.config";
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
    public StatusOperation checkJaasConnection() {
        final StatusOperation statusOperation = new StatusOperation("jaasCheck");

        logger.info("Start checkJaasConnection fileName[" + mJaasFile + "] JaasContent[" + mJaasContent + "]");
        String fileNameJaas = mJaasFile;
        String contentJaas = null;
        File temporaryFile = null;
        final String currentVariable = System.getProperty(JAAS_ENV_VARIABLE);
        try
        {
            if (mJaasContent != null && mJaasContent.trim().length() > 0)
            {
                temporaryFile = File.createTempFile("temp-jaasstandard", ".cfg");
                final BufferedWriter writer = new BufferedWriter(new FileWriter(temporaryFile));
                writer.write(mJaasContent);
                writer.close();
                logger.info("Get a JaasContent, write it a temporary file " + temporaryFile.getAbsolutePath());

                fileNameJaas = temporaryFile.getAbsolutePath();
                contentJaas = mJaasContent;
            }
            else
            {
                logger.info("load the JaasStandard file" + mJaasFile);

                contentJaas = loadFile(mJaasFile, this);
                if (contentJaas != null) {
                    contentJaas = contentJaas.replace("\n", "<br>");
                    logger.info("load the JaasStandard file" + mJaasFile + " content[" + contentJaas + "]");
                }

            }
            if (contentJaas == null || contentJaas.trim().length() == 0)
            {
                logger.info("no Jaas Content");
                statusOperation.addError("No JAAS content");
                return statusOperation;
            }

            System.setProperty(JAAS_ENV_VARIABLE, fileNameJaas);

            final CallbackHandler handler = new BonitaAuthenticationCallbackHandler(mJaasUserName, mJaasPassWord);
            String passwordToLog = "*** <click on ShowPassword to see the password in the log>";
            if (mShowPassword) {
                passwordToLog = mJaasPassWord;
            }
            logger.info("Connect [" + mJaasUserName + "] Password[" + passwordToLog + "] AuthKey[" + mJaasAuthentKey + "]");
            // reset the configuration to null to reaload it
            Configuration.setConfiguration(null);

            final LoginContext loginContext = new LoginContext(mJaasAuthentKey, handler);
            boolean isLogged = false;
            String status = "";
            try
            {
                loginContext.login();
                status += "Login with success;";
                isLogged = true;
            } catch (final LoginException e) {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                final String exceptionDetails = sw.toString();
                logger.severe("Exception e " + e.toString() + " at " + exceptionDetails);
                statusOperation.addError("Login: Exception JAAS " + e.toString());
            }
            try {
                if (isLogged)
                {
                    loginContext.logout();
                    status += "Logout with success;";
                }
            } catch (final LoginException e)
            {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                final String exceptionDetails = sw.toString();
                logger.severe("Exception e " + e.toString() + " at " + exceptionDetails);
                statusOperation.addError("Logout: Exception JAAS " + e.toString());

            }
            statusOperation.setSuccess("Connection with success");
            logger.info("Connection with success");

            /*
             * SUser user; try { user =
             * this.identityService.getUserByUserName(mUserName); } catch
             * (SUserNotFoundException e) {
             * finalResult.append("Error Identity "+e.toString()); } throw new
             * AuthenticationException(); }
             * if (this.logger.isLoggable(getClass(), TechnicalLogSeverity.TRACE)) {
             * this.logger.log(getClass(), TechnicalLogSeverity.TRACE, LogUtil
             * .getLogAfterMethod(getClass(), "checkUserCredentials")); }
             */

        } catch (final LoginException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("Exception e " + e.toString() + " at " + exceptionDetails);
            statusOperation.addError("Exception JAAS " + e.toString());

        } catch (final IOException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("Exception e " + e.toString() + " at " + exceptionDetails);
            statusOperation.addError("Exception Temporary file " + e.toString());
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("Exception e " + e.toString() + " at " + exceptionDetails);
            statusOperation.addError("Exception " + e.toString());


        } finally
        {
            System.setProperty(JAAS_ENV_VARIABLE, currentVariable == null ? "" : currentVariable);

            if (temporaryFile != null) {
                temporaryFile.delete();
            }

        }
        return statusOperation;
    }

    /**
     * @param userProvider
     * @param userFilter
     * @param useSSL
     * @param userName
     * @param password
     * @param logger
     * @return
     */
    public StatusOperation checkJaasOperation() {
        final StatusOperation statusOperation = new StatusOperation("JaasOperation");

        logger.info("Check LDAP userProvider[" + mJaasOpUserProvider + "] userFilter[" + mJaasOpUserFilter + "] userName[" + mJaasOpUserName + "] password["
                + mJaasOpPassword + "] useSSL["
                + mJaasOpUseSSL + "];<br>");

        SearchControls constraints = null;
        final Hashtable<String, Object> ldapEnvironment = new Hashtable<String, Object>(9);
        LdapContext ctx;
        final Pattern USERNAME_PATTERN = Pattern.compile("\\{USERNAME\\}");

        ldapEnvironment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        ldapEnvironment.put(Context.PROVIDER_URL, mJaasOpUserProvider);
        if (mJaasOpUseSSL) {
            ldapEnvironment.put(Context.SECURITY_PROTOCOL, "ssl");
        } else {
            ldapEnvironment.remove(Context.SECURITY_PROTOCOL);
        }
        final Matcher identityMatcher = USERNAME_PATTERN.matcher(mJaasOpIdentity);

        final String id = replaceUsernameToken(identityMatcher, mJaasOpIdentity, mJaasOpUserName);
        logger.info("Match [" + mJaasOpIdentity + "] with userName[" + mJaasOpUserName + "];<br> => Login:[" + id + "];<br>");

        ldapEnvironment.put(Context.SECURITY_CREDENTIALS, mJaasOpPassword);
        ldapEnvironment.put(Context.SECURITY_PRINCIPAL, id);

        try {
            // Connect to the LDAP server (using simple bind)
            ctx = new InitialLdapContext(ldapEnvironment, null);
            statusOperation.addDetails("################## Step 1: Connection successful;");

            constraints = new SearchControls();
            constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
            constraints.setReturningAttributes(new String[0]); // return no
                                                               // attrs
            constraints.setReturningObjFlag(true); // to get the full DN

            final Matcher filterMatcher = USERNAME_PATTERN.matcher(mJaasOpUserFilter);
            final String searchBy = replaceUsernameToken(filterMatcher, mJaasOpUserFilter, mJaasOpUserName);
            statusOperation.addDetails("SearchBy[" + searchBy + "];<br>");

            final NamingEnumeration<SearchResult> results = ctx.search("", searchBy, constraints);
            statusOperation.addDetails("################## Step 2: Search without error;<br>");

            // Extract the distinguished name of the user's entry
            // (Use the first entry if more than one is returned)
            if (results.hasMore()) {
                final SearchResult entry = results.next();

                // %%% - use the SearchResult.getNameInNamespace method
                // available in JDK 1.5 and later.
                // (can remove call to constraints.setReturningObjFlag)
                final String userDN = ((Context) entry.getObject()).getNameInNamespace();

                statusOperation.addDetails("################### Step 3 : found entry: " + userDN + "; SUCCESS<p>");
                final StringBuffer finalResult = new StringBuffer();
                finalResult.append("BonitaAuthentication-1 {<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;com.sun.security.auth.module.LdapLoginModule REQUIRED<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;userProvider=\"" + mJaasOpUserProvider + "\"<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;userFilter=\"" + mJaasOpUserFilter + "\"<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;authIdentity=\"" + mJaasOpIdentity + "\"<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;debug=true<br>");
                finalResult.append("&nbsp;&nbsp;&nbsp;useSSL=" + (mJaasOpUseSSL ? "true" : "false") + "<br>");
                finalResult.append(" }");
                statusOperation.mStatusResultJson.put("jaascontent", finalResult.toString());

            } else {
                statusOperation.addDetails("################# Step 3 : No result found !;");
                statusOperation.addError("No result found");

            }
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            statusOperation.addError("Exception " + e.toString() + "<br>" + exceptionDetails);

            logger.severe("Exception " + e.toString());
        }
        return statusOperation;
    }


    /* ******************************************************************************** */
    /*                                                                                  */
    /* Check method Get environement */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    /**
     * getTheEnvironnementJaasConnection
     *
     * @param jaasCheck
     * @return
     */
    public StatusOperation getEnvironnementJaasConnection()
    {
        final StatusOperation statusOperation = new StatusOperation("jassEnvironement");
        String jaasFile = null;
        final String contentFile = null;
        File temporaryFile = null;
        final String currentVariable = System.getProperty(JAAS_ENV_VARIABLE);
        try
        {
            jaasFile = System.getProperty("java.security.auth.login.config");
            statusOperation.mStatusResultJson.put("jaasvariable", jaasFile + "D");

            if (mJaasFile != null && mJaasFile.trim().length() > 0)
            {
                statusOperation.mStatusinfo = "Use JAAS file[" + mJaasFile + "];";
                jaasFile = mJaasFile;
            }
            if (jaasFile == null)
            {
                statusOperation.listEvents.add(new BEvent(eventNoJaasEnvironnement, "java.security.auth.login.config"));
                statusOperation.mStatusError = "No Jaas Configuration";
                return statusOperation;
            }

            final String contentJaas = loadFile(jaasFile, this);
            if (contentJaas == null) {
                statusOperation.listEvents.add(new BEvent(eventNoJaasConfirgurationFound, "File[" + jaasFile + "]"));
                statusOperation.mStatusError = "No Jaas File File exists [" + jaasFile + "]";
                return statusOperation;
            }
            statusOperation.mStatusinfo += "Jaas configuration OK;";
            statusOperation.mStatusResultJson.put("jaascontent", contentJaas);

            if (mJaasUserName == null)
            {
                statusOperation.mStatusinfo += "No User/Password to check Jaas Module;";
                return statusOperation;
            }

            // Ok, now we look on the File, and we try to connect each JAAS source
            String jaasSource = contentJaas;
            final int posAuthKey = jaasSource.indexOf(mJaasAuthentKey);
            if (posAuthKey != -1)
            {
                jaasSource = jaasSource.substring(posAuthKey);
            }
            final int posBegBrace = jaasSource.indexOf("{");
            final int posEndBrace = jaasSource.indexOf("}");
            if (posBegBrace == -1 || posEndBrace == -1)
            {
                statusOperation.listEvents.add(new BEvent(eventBadJaasStructure, (posBegBrace == -1 ? "{} expected;" : "")
                        + (posEndBrace == -1 ? "} expected" : "")));
                statusOperation.mStatusError = "Bad Jaas Structure {} expected [" + jaasFile + "]";
                return statusOperation;

            }
            jaasSource = jaasSource.substring(posBegBrace, posEndBrace - posBegBrace);
            logger.info("jaasSource = [" + jaasSource + "]");

            // now, parse all sources
            final List<Map<String, Object>> statusJaasModule = new ArrayList<Map<String, Object>>();
            final StringTokenizer st = new StringTokenizer(jaasSource, ";");
            while (st.hasMoreTokens())
            {
                final Map<String, Object> oneJaasModule = new HashMap<String, Object>();
                final String oneJaasSource = st.nextToken();

                final StringTokenizer stModule = new StringTokenizer(oneJaasSource);
                final String jaasModule = stModule.hasMoreTokens() ? stModule.nextToken() : "";
                final String jaasControl = stModule.hasMoreTokens() ? stModule.nextToken() : "";
                oneJaasModule.put("module", jaasModule);
                oneJaasModule.put("control", jaasControl);
                oneJaasModule.put("source", oneJaasSource);

                // can the user can connect with that ?
                final String useTemporaryPiece = "CraneTruck { " + oneJaasSource + "; }";
                temporaryFile = File.createTempFile("temp-jaasItem", ".cfg");
                logger.info("Created  = [" + temporaryFile + "]");

                final BufferedWriter writer = new BufferedWriter(new FileWriter(temporaryFile));
                writer.write(useTemporaryPiece);
                writer.close();
                logger.info("set [" + useTemporaryPiece + "] in the temporary file " + temporaryFile.getAbsolutePath());

                final String fileNamePiece = temporaryFile.getAbsolutePath();

                System.setProperty(JAAS_ENV_VARIABLE, fileNamePiece);

                final CallbackHandler handler = new BonitaAuthenticationCallbackHandler(mJaasUserName, mJaasPassWord);
                logger.info("Connect [" + mJaasUserName + "] to [" + jaasModule + "] ");
                // reset the configuration to null to reaload it
                Configuration.setConfiguration(null);
                try
                {
                    final LoginContext loginContext = new LoginContext("CraneTruck", handler);
                    loginContext.login();
                    loginContext.logout();
                    oneJaasModule.put("status", "CONNECTION");
                    oneJaasModule.put("style", "background-color:#B5E61D"); // GREEN


                } catch (final LoginException e) {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    final String exceptionDetails = sw.toString();
                    logger.info("Login failed e " + e.toString() + " at " + exceptionDetails);
                    oneJaasModule.put("status", "REFUSED");
                    if ("required".equalsIgnoreCase(jaasControl)) {
                        oneJaasModule.put("style", "background-color:#FF0000"); // RED
                    }
                    else {
                        oneJaasModule.put("style", "background-color:#C49E71"); // Brown
                    }

                } catch (final Exception e)
                {
                    final StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    final String exceptionDetails = sw.toString();
                    logger.info("Login failed e " + e.toString() + " at " + exceptionDetails);
                    oneJaasModule.put("status", "ERROR");
                    if ("required".equalsIgnoreCase(jaasControl)) {
                        oneJaasModule.put("style", "background-color:#FF0000"); // RED
                    }
                    else {
                        oneJaasModule.put("style", "background-color:#C49E71"); // Brown
                    }

                }
                temporaryFile.delete();
                temporaryFile = null;
            }
            statusOperation.mStatusResultJson.put("jaasconnection", statusJaasModule);

            statusOperation.mStatusinfo += "Status JAAS Module done;";

        } catch (final Exception e)
        {
            statusOperation.listEvents.add(new BEvent(eventEnvironnementError, "file [" + contentFile + "] "));
        }
        if (temporaryFile != null) {
            temporaryFile.delete();
        }
        System.setProperty(JAAS_ENV_VARIABLE, currentVariable == null ? "" : currentVariable);
        return statusOperation;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Toolbox */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * @param matcher
     * @param string
     * @param userName
     * @return
     */
    private String replaceUsernameToken(final Matcher matcher, final String string, final String userName) {
        return matcher != null ? matcher.replaceAll(userName) : string;
    }

    /**
     * load the file, and retrun the content of in a String. When an error occure,
     * the result is null.
     */
    public String loadFile(final String fileName,
            final Object caller)
    {
        final StringBuffer result = new StringBuffer();
        try
        {
            final FileReader fileReader = new FileReader(fileName);
            int nbRead;
            final char[] buffer = new char[50000];
            while ((nbRead = fileReader.read(buffer, 0, 50000)) > 0) {
                result.append(new String(buffer).substring(0, nbRead));
            }
            fileReader.close();
            return result.toString();
        } catch (final Exception e)
        {
            return null;
        }
    }

}
