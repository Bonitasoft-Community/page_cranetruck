package com.bonitasoft.custompage.cranetruck;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONValue;

import com.bonitasoft.custompage.cranetruck.ToolFileProperties.PropertiesParam;
import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class PropertiesLogger implements PropertiesParam {

    static Logger logger = Logger.getLogger(PropertiesLdapConnection.class.getName());
    private String mLogDirPath;
    private String mFileDatePrefix;
    private String mLogLevel;

    private final StatusOperation mStatusOperation;

    public PropertiesLogger() {
        mStatusOperation = new StatusOperation("Logger");
    }

    public String getTitle() {
        return mStatusOperation.mStatusTitle;
    }

    public static PropertiesLogger getInstanceFromJsonSt(final String jsonSt) {
        final PropertiesLogger propertiesLogger = new PropertiesLogger();
        if (jsonSt == null) {
            return propertiesLogger;
        }
        logger.info("PropertiesLogger: JsonSt[" + jsonSt + "]");
        final Map<String, Object> jsonHash = (Map<String, Object>) JSONValue.parse(jsonSt);
        if (jsonHash == null) {
            return propertiesLogger;
        }
        return getInstanceFromMap(jsonHash);
    }

    /**
     * @param map
     * @return
     */
    public static PropertiesLogger getInstanceFromMap(final Map<String, Object> map) {
        final PropertiesLogger ldapConnectionParam = new PropertiesLogger();
        if (map == null) {
            ldapConnectionParam.mStatusOperation.mStatusError = "No parameters from the map";
            return ldapConnectionParam;
        }

        ldapConnectionParam.mLogDirPath = (String) map.get("log_dir_path");
        ldapConnectionParam.mFileDatePrefix = (String) map.get("log_file_date_prefix");
        ldapConnectionParam.mLogLevel = (String) map.get("log_level");
        ldapConnectionParam.checkErrors();
        return ldapConnectionParam;
    }

    /**
     *
     */
    public Map<String, Object> toMap() {
        final HashMap<String, Object> result = new HashMap<String, Object>();
        result.put("log_dir_path", mLogDirPath);
        result.put("log_file_date_prefix", mFileDatePrefix);
        result.put("log_level", mLogLevel);

        return result;
    }

    /**
     * instance the object from the properties file
     */
    public static PropertiesLogger getInstanceFromProperties(final Properties properties) {
        final PropertiesLogger propertiesLogger = new PropertiesLogger();
        propertiesLogger.readFromProperties(properties);

        propertiesLogger.checkErrors();
        return propertiesLogger;
    }

    /**
     * check the parameters, and return a status
     *
     * @return
     */
    public StatusOperation checkErrors() {
        mStatusOperation.mStatusError = "";
        try {
            Level.parse(mLogLevel);
        } catch (final Exception e) {
            addError("Unknow LEVEL [" + mLogLevel + "] in loglevel");
        }

        return mStatusOperation;
    }

    public StatusOperation getStatusOperation() {
        return mStatusOperation;
    };

    @Override
    public String toString() {
        return "LoggerProperties:";
    }

    private static final String LOG_DIR_PATH = "log_dir_path";
    private static final String LOG_FILE_DATE_PREFIX = "log_file_date_prefix";
    private static final String LOG_LEVEL = "log_level";

    /**
     *
     */
    public void readFromProperties(final Properties properties) {
        mLogDirPath = properties.getProperty(LOG_DIR_PATH);
        mFileDatePrefix = properties.getProperty(LOG_FILE_DATE_PREFIX);
        mLogLevel = properties.getProperty(LOG_LEVEL);
        mStatusOperation.mStatusinfo = "Properties Logger loaded";

        logger.info("propertiesLogger: Read from properties logPath[" + mLogDirPath + "]");
    }

    public void writeInProperties(final Properties properties) {
        properties.setProperty(LOG_DIR_PATH, mLogDirPath);
        properties.setProperty(LOG_FILE_DATE_PREFIX, mFileDatePrefix);
        properties.setProperty(LOG_LEVEL, mLogLevel);

    }

    public boolean isSavedPropertiesFile() {
        return true;
    };

    public String getPropertiesFileName() {
        return CONFIG_FILE_NAME;
    }

    public void addError(final String error) {
        mStatusOperation.addError(error);

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
    public StatusOperation checkLoggerConnection() {
        logger.info("Start checkLogger=" + toString());

        final StatusOperation statusOperation = getStatusOperation();
        if (statusOperation.isError()) {
            return statusOperation;
        }
        statusOperation.mStatusinfo = "OK";

        return statusOperation;

    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Properties file */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    private static final String CONFIG_FILE_NAME = "logger.properties";

}
