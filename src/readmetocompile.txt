This project use some Bonita Subscription Edition to compile.
Theses library are not part of the code saved in Github
So, you have to get them from a Subscription Studio, copy here and refererence in yor project.

The needed librairy are:
 - bonita-client-sp-x.y.z.jar
 - bonita-common-sp-x.y.z.jar
 - bonita-ldap-synchronizer-x.y.z.jar
 
 Nota : the last one is part of the tool LDAP Synchronizer tool
 
 Load them in your local maven
 mvn install:install-file -Dfile=C:\atelier\BPM-SP-7.2.0\workspace\tomcat\webapps\bonita\WEB-INF\lib\bonita-client-sp-7.2.0.jar -DgroupId=com.bonitasoft.engine  -DartifactId=bonita-client-sp -Dversion=7.2.0 -Dpackaging=jar
 mvn install:install-file -Dfile=C:\atelier\BPM-SP-7.2.0\workspace\tomcat\webapps\bonita\WEB-INF\lib\bonita-common-sp-7.2.0.jar -DgroupId=com.bonitasoft.engine  -DartifactId=bonita-common-sp -Dversion=7.2.0 -Dpackaging=jar
 mvn install:install-file -Dfile="C:\atelier\LDAP-Synchronizer 7.0.3\BonitaBPMSubscription-7.0.3-LDAP-Synchronizer\lib\bonita-ldap-synchronizer-7.0.3.jar" -DgroupId=com.bonitasoft.engine  -DartifactId=bonita-ldap-synchronizer -Dversion=7.2.0 -Dpackaging=jar
 
