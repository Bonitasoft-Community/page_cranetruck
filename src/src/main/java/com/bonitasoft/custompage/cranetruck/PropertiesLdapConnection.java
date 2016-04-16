package com.bonitasoft.custompage.cranetruck;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.ToolFileProperties.PropertiesParam;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class PropertiesLdapConnection implements PropertiesParam {

    static Logger logger = Logger.getLogger(PropertiesLdapConnection.class.getName());

    public String mHostURL;
    public String mAuthType;
    public String mPrincipalDN;
    public String mPassword;
    public String mDirectoryUserType;
    public String mSearchDN;
    public String mSearchFilter;
    public boolean mUsepagedSearch = true;
    public int mPageSize = 1000;

    private final StatusOperation mStatusOperation;

    public PropertiesLdapConnection()
    {
        mStatusOperation = new StatusOperation("Ldap Connection");
    }

    public String getTitle()
    {
        return mStatusOperation.mStatusTitle;
    }

    /**
     * check the parameters, and return a status
     *
     * @return
     */
    public StatusOperation checkErrors() {
        mStatusOperation.mStatusError = "";

        if (mHostURL == null) {
            mStatusOperation.mStatusError += "HostUrl is mandatory";
        }

        return mStatusOperation;
    }

    public StatusOperation getStatusOperation()
    {
        return mStatusOperation;
    };

    @Override
    public String toString()
    {
        return "HostUrl[" + mHostURL + "] AuthType[" + mAuthType + "] PrincipalDN[" + mPrincipalDN + "] Password[" + mPassword
                + "] SearchDN[" + mSearchDN + "] searchFilter[" + mSearchFilter + "] UsePagedSearch[" + mUsepagedSearch + "] PageSize[" + mPageSize + "]";
    }

    public void addError(final String error) {
        mStatusOperation.mStatusError += error + ";";

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* getinstance */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public static PropertiesLdapConnection getInstanceFromJsonSt(final String jsonSt) {
        final PropertiesLdapConnection ldapConnectionParam = new PropertiesLdapConnection();
        if (jsonSt == null) {
            return ldapConnectionParam;
        }
        logger.info("LdapConnectionParam: JsonSt[" + jsonSt + "]");
        final Map<String, Object> jsonHash = (Map<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return ldapConnectionParam;
        }
        return getInstanceFromMap(jsonHash);
    }

    public static PropertiesLdapConnection getInstanceFromMap(final Map<String, Object> map) {
        final PropertiesLdapConnection ldapConnectionParam = new PropertiesLdapConnection();
        if (map == null)
        {
            ldapConnectionParam.mStatusOperation.mStatusError = "No parameters from the map";
            return ldapConnectionParam;
        }
        ldapConnectionParam.readFromMap(map);
        ldapConnectionParam.checkErrors();
        return ldapConnectionParam;
    }

    /**
     * instance the object from the properties file
     */
    public static PropertiesLdapConnection getInstanceFromProperties(final Properties properties) {
        final PropertiesLdapConnection ldapConnectionParam = new PropertiesLdapConnection();
        ldapConnectionParam.readFromProperties(properties);

        ldapConnectionParam.checkErrors();
        return ldapConnectionParam;
    }


    /* ******************************************************************************** */
    /*                                                                                  */
    /* Map management */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public Map<String, Object> toMap()
    {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("hosturl", mHostURL);
        result.put("authtype", mAuthType);
        result.put("principaldn", mPrincipalDN);
        result.put("password", mPassword);
        result.put("directory_user_type", mDirectoryUserType);
        result.put("used_paged_search", mUsepagedSearch);
        result.put("pagesize", mPageSize);
        result.put("searchdn", mSearchDN);
        result.put("searchfilter", mSearchFilter);
        return result;
    }

    /**
     * read from the Map
     *
     * @param map
     */
    public void readFromMap(final Map<String, Object> map)
    {
        mHostURL = (String) map.get("hosturl");
        mAuthType = (String) map.get("authtype");
        mPrincipalDN = (String) map.get("principaldn");
        mPassword = (String) map.get("password");
        mDirectoryUserType = (String) map.get("directory_user_type");

        mUsepagedSearch = Toolbox.getBoolean(map.get("used_paged_search"), false);
        mPageSize = Toolbox.getInteger(map.get("pagesize"), 100);

        mSearchDN = (String) map.get("searchdn");
        mSearchFilter = (String) map.get("searchfilter");
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties management */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    private static final String CONFIG_FILE_NAME = "ldap.properties";
    private static String domain;

    // Properties
    private static final String PROP_HOST_URL = "host_url";
    private static final String PROP_AUTH_TYPE = "auth_type";
    private static final String PROP_PRINCIPAL_DN = "principal_dn";
    private static final String PROP_PRINCIPAL_PASSWORD = "principal_password";
    private static final String PROP_DIRECTORY_USER_TYPE = "directory_user_type";
    private static final String PROP_USE_PAGED_SEARCH = "use_paged_search";
    private static final String PROP_PAGE_SIZE = "page_size";
    private static final String PROP_TEST_SEARCH_DN = "test_search_dn";
    private static final String PROP_TEST_SEARCH_FILTER = "test_search_filter";

    public void readFromProperties(final Properties properties) {
        mHostURL = properties.getProperty(PROP_HOST_URL);
        mAuthType = properties.getProperty(PROP_AUTH_TYPE);
        mPrincipalDN = properties.getProperty(PROP_PRINCIPAL_DN);
        mPassword = properties.getProperty(PROP_PRINCIPAL_PASSWORD);
        mDirectoryUserType = properties.getProperty(PROP_DIRECTORY_USER_TYPE);
        mUsepagedSearch = Toolbox.getBoolean(properties.getProperty(PROP_USE_PAGED_SEARCH), false);
        mPageSize = Toolbox.getInteger(properties.getProperty(PROP_PAGE_SIZE), 100);
        mStatusOperation.mStatusinfo = "Properties LdapConnection loaded";
        mSearchDN = properties.getProperty(PROP_TEST_SEARCH_DN);
        mSearchFilter = properties.getProperty(PROP_TEST_SEARCH_FILTER);
    }

    public void writeInProperties(final Properties properties)
    {
        properties.setProperty(PROP_HOST_URL, mHostURL);
        properties.setProperty(PROP_AUTH_TYPE, mAuthType);
        properties.setProperty(PROP_PRINCIPAL_DN, mPrincipalDN);
        properties.setProperty(PROP_PRINCIPAL_PASSWORD, mPassword);
        properties.setProperty(PROP_DIRECTORY_USER_TYPE, mDirectoryUserType);
        properties.setProperty(PROP_USE_PAGED_SEARCH, String.valueOf(mUsepagedSearch));
        properties.setProperty(PROP_PAGE_SIZE, String.valueOf(mPageSize));
        properties.setProperty(PROP_TEST_SEARCH_DN, mSearchDN);
        properties.setProperty(PROP_TEST_SEARCH_FILTER, mSearchFilter);

    }

    public boolean isSavedPropertiesFile() {
        return true;
    };

    public String getPropertiesFileName() {
        return CONFIG_FILE_NAME;
    }


    /* ******************************************************************************** */
    /*                                                                                  */
    /* Test file */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public DirContext connect() throws NamingException
    {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, mHostURL);
        env.put(Context.SECURITY_AUTHENTICATION, mAuthType);
        env.put(Context.SECURITY_PRINCIPAL, mPrincipalDN);
        env.put(Context.SECURITY_CREDENTIALS, mPassword);
        final DirContext context = new InitialDirContext(env);
        return context;

    }
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
    public StatusOperation checkLdapConnection() {
        logger.info("Start checkLdapConnection ldapConnectionParam=" + toString());


        final StatusOperation statusOperation = getStatusOperation();
        if (statusOperation.isError())
        {
            return statusOperation;
        }
        final StringBuffer finalResult = new StringBuffer();
        try {

            final DirContext context = connect();
            finalResult.append("Connected");
            statusOperation.mStatusResultJson.put("connection", "Connection correct");
            final ArrayList<HashMap<String, Object>> resultSearch = new ArrayList<HashMap<String, Object>>();

            if (mSearchDN != null && mSearchDN.trim().length() > 0) {
                final SearchControls constraints = new SearchControls();
                constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);
                constraints.setReturningAttributes(null);

                final NamingEnumeration<SearchResult> results = context.search(mSearchDN, mSearchFilter, constraints);
                while (results.hasMore()) {
                    final HashMap<String, Object> oneRecord = new HashMap<String, Object>();
                    final SearchResult searchResult = results.next();
                    final Attributes attributes = searchResult.getAttributes();
                    final NamingEnumeration<Attribute> attribEnum = (NamingEnumeration<Attribute>) attributes.getAll();
                    while (attribEnum.hasMore()) {
                        final Attribute attribute = attribEnum.next();
                        final String ldapAttributeId = attribute.getID();
                        // Ignore non string and multi value attributes
                        if (attribute.get() instanceof String && attribute.size() == 1) {
                            final String attributeValue = (String) attribute.get();

                            oneRecord.put(ldapAttributeId, attributeValue);
                        }
                    }
                    resultSearch.add(oneRecord);
                    statusOperation.mStatusDetails += "Number record found " + resultSearch.size();
                }
                statusOperation.mStatusResultJson.put("search", "Found " + resultSearch.size());

                statusOperation.mStatusResultJson.put("searchlist", resultSearch);

            }
            context.close();

            statusOperation.mStatusinfo = "OK";
            statusOperation.mStatusDetails = "Connection success";
            return statusOperation;

        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("LdapConnection:" + e.toString() + " detail: " + sw.toString());

            finalResult.append("Exception :" + e.toString());
            statusOperation.mStatusError = "Exception " + e.toString();
            statusOperation.mStatusResultJson.put("error", "Exception " + e.toString());
            return statusOperation;
        }
    }



}
