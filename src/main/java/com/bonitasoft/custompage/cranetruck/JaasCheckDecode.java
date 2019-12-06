package com.bonitasoft.custompage.cranetruck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.bonitasoft.custompage.cranetruck.Toolbox.StatusOperation;

public class JaasCheckDecode {

    /**
     * 
     * BonitaAuthentication-1 {
    com.sun.security.auth.module.LdapLoginModule REQUIRED
    userProvider="ldap://localhost:10389/ou=People,dc=example,dc=com"
    userFilter="(&(uid={USERNAME})(objectClass=inetOrgPerson))"
    authzIdentity="{USERNAME}"
    debug=true
    useSSL=false;
     * 
     *==> module = com.sun.security.auth.module.LdapLoginModule
     *==> operation = REQUIRED
     *==> options= userProvider, userFilter, authzIdentity, debug, useSSL
     *
     */
    public static class JaasSource {
        public String module="";
        public String control="";
        public Map<String,String> options = new HashMap<String,String>();
        public String source="";
    }
    private String jaasContent;
    //decode a Jaas Content file
    public JaasCheckDecode( String jaasContent) {
        this.jaasContent = jaasContent;
    }
    /**
     * from a key, return the list of source detected
     * @param key
     * @return
     */
    public List<JaasSource> getJaasSource( String jaasKey, StatusOperation statusOperation )
    {
        List<JaasSource> listJaasSource= new ArrayList<JaasSource>();
        String jaasSourceSt = jaasContent;
        final int posAuthKey = jaasSourceSt.indexOf(jaasKey);
        if (posAuthKey != -1) {
            jaasSourceSt = jaasSourceSt.substring(posAuthKey);
        }
        final int posBegBrace = jaasSourceSt.indexOf("{");
        // don't try to find the end brace : data (like userFilter) can contains a end brace !
        if (posBegBrace == -1) {
            statusOperation.addDetails("Bad structure : { expected");
            return listJaasSource;

        }
        jaasSourceSt = jaasSourceSt.substring(posBegBrace+1);
       
        // now, parse all sources
        final StringTokenizer st = new StringTokenizer(jaasSourceSt, ";");
        while (st.hasMoreTokens()) {
            JaasSource jaasSource = new JaasSource();            
            jaasSource.source = st.nextToken();

            final StringTokenizer stModule = new StringTokenizer(jaasSource.source);
            jaasSource.module = stModule.hasMoreTokens() ? stModule.nextToken() : "";
            jaasSource.control = stModule.hasMoreTokens() ? stModule.nextToken() : "";
            // user finish the source by ; } : so the next source is not a real one
            if (jaasSource.module.equals("}"))
                break;
            listJaasSource.add(jaasSource);
            
            // options now
            while (stModule.hasMoreTokens())
            {
                String oneOption = stModule.nextToken();
                final int posBegEgal = oneOption.indexOf("=");
                if (posBegEgal != -1)
                {
                    String nameOption = oneOption.substring(0,posBegEgal);
                    String valueOption = oneOption.substring(posBegEgal+1);
                    jaasSource.options.put(nameOption.trim(), valueOption.trim());
                }
 
            }
            
        }
        return listJaasSource;
    }
}
