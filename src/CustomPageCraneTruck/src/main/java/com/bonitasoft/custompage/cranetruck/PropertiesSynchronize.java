package com.bonitasoft.custompage.cranetruck;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.naming.directory.DirContext;

import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.ToolFileProperties.PropertiesParam;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;
import com.bonitasoft.ldapsynchronizer.data.LDAPSearchParameters;
import com.bonitasoft.ldapsynchronizer.data.RawUserData;
import com.bonitasoft.ldapsynchronizer.repository.LDAPUserRepository;

public class PropertiesSynchronize implements PropertiesParam {

    static Logger logger = Logger.getLogger(PropertiesLdapConnection.class.getName());

    private String mErrorLevelUponFaillingToGetRelatedUser;
    private List<Map<String, String>> mListWatchedDir = new ArrayList<Map<String, String>>();
    private String mBonitaUserNameCase;
    private String mBonitaNoSyncUser;
    private String mBonitaRemoveUser;
    private String mBonitaDeactivateUsers;
    private String mBonitaUserRole;
    private Boolean mAllowRecursiveGroups;
    private List<Map<String, String>> mListGroups = new ArrayList<Map<String, String>>();
    private List<Map<String, String>> mLdapSearchFilterGroup = new ArrayList<Map<String, String>>();

    private final StatusOperation mStatusOperation;

    public PropertiesSynchronize()
    {
        mStatusOperation = new StatusOperation("Sync");
    }

    public String getTitle()
    {
        return mStatusOperation.mStatusTitle;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* getinstance */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * getInstanceFromJsonSt
     *
     * @param jsonSt
     * @return
     */
    public static PropertiesSynchronize getInstanceFromJsonSt(final String jsonSt) {
        final PropertiesSynchronize ldapConnectionParam = new PropertiesSynchronize();
        if (jsonSt == null) {
            return ldapConnectionParam;
        }
        logger.info("LdapConnectionParam: JsonSt[" + jsonSt + "]");
        @SuppressWarnings("unchecked")
        final Map<String, Object> jsonHash = (Map<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return ldapConnectionParam;
        }
        return getInstanceFromMap(jsonHash);
    }

    /**
     * getInstance form map
     *
     * @param map
     * @return
     */
    public static PropertiesSynchronize getInstanceFromMap(final Map<String, Object> map) {
        final PropertiesSynchronize propertiesParam = new PropertiesSynchronize();
        if (map == null)
        {
            propertiesParam.mStatusOperation.mStatusError = "No parameters from the map";
            return propertiesParam;
        }
        propertiesParam.readFromMap(map);

        propertiesParam.checkErrors();
        return propertiesParam;
    }

    /**
     * instance the object from the properties file
     */
    public static PropertiesSynchronize getInstanceFromProperties(final Properties properties) {
        final PropertiesSynchronize ldapConnectionParam = new PropertiesSynchronize();
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

    /**
     * to map
     */
    public Map<String, Object> toMap()
    {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("error_level_upon_failing_to_get_related_user", mErrorLevelUponFaillingToGetRelatedUser);
        result.put("ldap_watched_directories", mListWatchedDir);
        result.put("bonita_username_case_list", mBonitaUserNameCase);
        result.put("bonita_nosync_users", mBonitaNoSyncUser);
        result.put("bonita_remove_users", mBonitaRemoveUser);
        result.put("bonita_deactivate_users", mBonitaDeactivateUsers);
        result.put("bonita_user_role", mBonitaUserRole);
        result.put("allow_recursive_groups", mAllowRecursiveGroups);
        result.put("ldap_groups", mListGroups);
        result.put("ldap_searchs", mLdapSearchFilterGroup);

        return result;
    }

    /**
     * read from the Map
     *
     * @param map
     */
    public void readFromMap(final Map<String, Object> map)
    {
        logger.info("PropertiesSynchronize : " + map.toString());
        mErrorLevelUponFaillingToGetRelatedUser = Toolbox.getString(map.get("error_level_upon_failing_to_get_related_user"), "");
        mListWatchedDir = Toolbox.getList(map.get("ldap_watched_directories"), null);
        mBonitaUserNameCase = Toolbox.getString(map.get("bonita_username_case_list"), "");
        mBonitaNoSyncUser = Toolbox.getString(map.get("bonita_nosync_users"), "");
        mBonitaRemoveUser = Toolbox.getString(map.get("bonita_remove_users"), "");
        mBonitaDeactivateUsers = Toolbox.getString(map.get("bonita_deactivate_users"), "");
        mBonitaUserRole = Toolbox.getString(map.get("bonita_user_role"), "");
        mAllowRecursiveGroups = Toolbox.getBoolean(map.get("allow_recursive_groups"), Boolean.TRUE);
        mListGroups = Toolbox.getList(map.get("ldap_groups"), null);
        mLdapSearchFilterGroup = Toolbox.getList(map.get("ldap_searchs"), null);
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties management */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */


    // PROPERTIES FILE : User sync constants
    private static final String LDAP_WATCHED_DIRECTORIES = "ldap_watched_directories";
    private static final String LDAP_SEARCH_DN = "ldap_search_dn";
    private static final String LDAP_SEARCH_FILTER = "ldap_search_filter";

    private static final String BONITA_NOSYNC_USERS = "bonita_nosync_users";
    private static final String BONITA_REMOVE_USERS = "bonita_remove_users";
    private static final String BONITA_USER_ROLE = "bonita_user_role";
    private static final String BONITA_USER_ROLE_DEFAULT = "user";
    private static final String BONITA_REMOVE_USERS_DEFAULT = "false";

    // Case constants
    private static final String CASE_LABELS[] = { "mixed", "uppercase", "lowercase" };

    // Bonita user name case import configuration constants
    private static final String BONITA_USERNAME_CASE = "bonita_username_case";
    private static final String BONITA_USERNAME_CASE_DEFAULT = "mixed";
    private static final String BONITA_DEACTIVATE_USERS = "bonita_deactivate_users";
    private static final String ALLOW_RECURSIVE_GROUPS = "allow_recursive_groups";

    // Error reporting constants
    public static final int ERROR_LEVEL_IGNORE = 0;
    public static final int ERROR_LEVEL_WARN = 1;
    public static final int ERROR_LEVEL_FATAL = 2;
    private static final String ERROR_LEVEL_LABELS[] = { "ignore", "warn",
            "fatal" };

    // Error configuration constants
    private static final String ERROR_LEVEL_UPON_FAILING_TO_GET_RELATED_USER = "error_level_upon_failing_to_get_related_user";
    private static final int ERROR_LEVEL_UPON_FAILING_TO_GET_RELATED_USER_DEFAULT = ERROR_LEVEL_FATAL;

    // Group sync constants
    private static final String LDAP_GROUPS = "ldap_groups";
    private static final String LDAP_GROUP_DN = "ldap_group_dn";
    private static final String LDAP_GROUP_FORCED_BONITA_NAME = "forced_bonita_group_name";

    // Ldap search filter * NEW *
    private static final String LDAP_SEARCH_FILTER_GROUPS = "ldap_search_filter_groups";

    /**
     * read from properties
     */
    public void readFromProperties(final Properties properties) {
        mErrorLevelUponFaillingToGetRelatedUser = properties.getProperty(ERROR_LEVEL_UPON_FAILING_TO_GET_RELATED_USER);
        mListWatchedDir = ToolFileProperties.readListInPropertiesFile(LDAP_WATCHED_DIRECTORIES, new String[] { LDAP_SEARCH_DN, LDAP_SEARCH_FILTER },
                properties);
        mBonitaUserNameCase = properties.getProperty(BONITA_USERNAME_CASE);
        if (mBonitaUserNameCase == null) {
            mBonitaUserNameCase = BONITA_USERNAME_CASE_DEFAULT;
        }
        mBonitaNoSyncUser = properties.getProperty(BONITA_NOSYNC_USERS);
        mBonitaRemoveUser = properties.getProperty(BONITA_REMOVE_USERS);
        mBonitaDeactivateUsers = properties.getProperty(BONITA_DEACTIVATE_USERS);

        mBonitaUserRole = properties.getProperty(BONITA_USER_ROLE);
        if (mBonitaUserRole == null) {
            mBonitaUserRole = BONITA_USER_ROLE_DEFAULT;
        }
        mAllowRecursiveGroups = Toolbox.getBoolean(properties.getProperty(ALLOW_RECURSIVE_GROUPS), Boolean.TRUE);

        mListGroups = ToolFileProperties.readListInPropertiesFile(LDAP_GROUPS, new String[] { LDAP_GROUP_DN,
                LDAP_GROUP_FORCED_BONITA_NAME },
                properties);

        mLdapSearchFilterGroup = ToolFileProperties.readListInPropertiesFile(LDAP_SEARCH_FILTER_GROUPS, new String[] { LDAP_SEARCH_DN,
                LDAP_SEARCH_FILTER },
                properties);

    }

    /**
     * write to a properties
     */
    public void writeInProperties(final Properties properties)
    {
        properties.setProperty(ERROR_LEVEL_UPON_FAILING_TO_GET_RELATED_USER, mErrorLevelUponFaillingToGetRelatedUser);
        ToolFileProperties.writeListInPropertiesFile(LDAP_WATCHED_DIRECTORIES, mListWatchedDir, "dir", properties);

        properties.setProperty(BONITA_USERNAME_CASE, mBonitaUserNameCase);
        properties.setProperty(BONITA_NOSYNC_USERS, mBonitaNoSyncUser);
        properties.setProperty(BONITA_REMOVE_USERS, mBonitaRemoveUser);
        properties.setProperty(BONITA_DEACTIVATE_USERS, mBonitaDeactivateUsers);
        properties.setProperty(BONITA_USER_ROLE, mBonitaUserRole);
        properties.setProperty(ALLOW_RECURSIVE_GROUPS, mAllowRecursiveGroups.toString());

        ToolFileProperties.writeListInPropertiesFile(LDAP_GROUPS, mListGroups, "group", properties);
        ToolFileProperties.writeListInPropertiesFile(LDAP_SEARCH_FILTER_GROUPS, mLdapSearchFilterGroup, "search", properties);

    }

    public boolean isSavedPropertiesFile() {
        return true;
    };

    /**
     * check the parameters, and return a status
     *
     * @return
     */
    public StatusOperation checkErrors() {
        mStatusOperation.mStatusError = "";

        return mStatusOperation;
    }

    public StatusOperation getStatusOperation()
    {
        return mStatusOperation;
    };

    @Override
    public String toString()
    {
        return "LoggerProperties:";
    }


    private static final String CONFIG_FILE_NAME = "sync.properties";

    public String getPropertiesFileName() {
        return CONFIG_FILE_NAME;
    }

    public void addError(final String error) {
        mStatusOperation.mStatusError += error + ";";

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Tests */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public static class PropertiesSynchronizeTest {

        PropertiesSynchronize propertiesSynchronize;
        PropertiesLdapConnection propertiesLdapConnection;

        public static PropertiesSynchronizeTest getInstanceFromJsonSt(final String jsonSt)
        {
            final PropertiesSynchronizeTest propertiesSynchronizeTest = new PropertiesSynchronizeTest();
            if (jsonSt == null) {
                return propertiesSynchronizeTest;
            }
            logger.info("propertiesSynchronizeTest: JsonSt[" + jsonSt + "]");
            @SuppressWarnings("unchecked")
            final Map<String, Object> jsonHash = (Map<String, Object>) JSONValue.parse(jsonSt);
            if (jsonHash == null) {
                return propertiesSynchronizeTest;
            }

            // we get a Properties Synchronize
            propertiesSynchronizeTest.propertiesSynchronize = PropertiesSynchronize.getInstanceFromMap(jsonHash);
            // and a LDAP test
            propertiesSynchronizeTest.propertiesLdapConnection = PropertiesLdapConnection.getInstanceFromMap(jsonHash);
            propertiesSynchronizeTest.propertiesSynchronize.checkErrors();
            propertiesSynchronizeTest.propertiesLdapConnection.checkErrors();

            return propertiesSynchronizeTest;
        }

        public StatusOperation checkErrors() {
            final StatusOperation statusOperation = new StatusOperation("Test Synchronize");
            if (propertiesSynchronize != null) {
                statusOperation.addStatusOperation(propertiesSynchronize.checkErrors());
            }
            if (propertiesLdapConnection != null) {
                statusOperation.addStatusOperation(propertiesLdapConnection.checkErrors());
            }

            return statusOperation;
        }

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
    public static StatusOperation checkSynchronize(final PropertiesSynchronizeTest propertiesSynchronizeTest) {
        logger.info("Start checkSynchronize");

        final StatusOperation statusOperation = propertiesSynchronizeTest.checkErrors();

        if (statusOperation.isError())
        {
            return statusOperation;
        }

        final StringBuffer synthesis = new StringBuffer();
        final HashMap<String, Object> finalResult = new HashMap<String, Object>();
        try
        {
            statusOperation.mStatusinfo = "OK";
            final DirContext dirContext = propertiesSynchronizeTest.propertiesLdapConnection.connect();

            final ArrayList<HashMap<String, Object>> testWathDirectories = new ArrayList<HashMap<String, Object>>();
            finalResult.put("watchdirectories", testWathDirectories);
            // LDAP SEARCH USER
            for (int i = 0; i < propertiesSynchronizeTest.propertiesSynchronize.mListWatchedDir.size(); i++)
            {
                final Map<String, String> watchedDir = propertiesSynchronizeTest.propertiesSynchronize.mListWatchedDir.get(i);
                final HashMap<String, Object> oneRecord = new HashMap<String, Object>();
                testWathDirectories.add(oneRecord);

                // TODO synchronization par page pour avoir le nombre total d'utilisateur MAIS recuperer que le 1er element
                final LDAPSearchParameters searchParameters = new LDAPSearchParameters("search",  watchedDir.get( LDAP_SEARCH_DN), watchedDir.get( LDAP_SEARCH_FILTER));
                final String name = "WatchedDirectory " + i;
                oneRecord.put("name", name);
                try
                {
                    final List<RawUserData> listRowData = LDAPUserRepository.getUsers(searchParameters);
                    oneRecord.put("size", listRowData.size());
                    if (listRowData.size() > 0) {
                        oneRecord.put("example", listRowData.get(0).getData());
                    }
                } catch (final Exception e)
                {
                    statusOperation.addError("Watched directory " + name + " : " + e.toString());
                    oneRecord.put("error", e.toString());
                }

            }


        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            logger.severe("LdapConnection:" + e.toString() + " detail: " + sw.toString());

            synthesis.append("Exception :" + e.toString());
            statusOperation.mStatusError = "Exception " + e.toString();
        }
        finalResult.put("synthesis", synthesis.toString());
        statusOperation.mStatusResultJson = finalResult;
        return statusOperation;
    }

}
