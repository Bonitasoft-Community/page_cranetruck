'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('cranetruckmonitor', ['googlechart', 'ui.bootstrap','ngSanitize']);






// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('CraneTruckController',
	function ( $http, $scope, $sce ) {

	this.isshowhistory = false;
	this.hosturl = '';
	this.authtype='';
	this.principaldn='';
	this.password='';
	this.searchdn='';
	this.searchfilter = '';
	this.usedpagedsearch=true;
	this.data = { 'ldap':{}, 'bonita':{}, 'logger':{}, 'sync':{}, 'mapper':{} };
	this.display = { ldapconnection:true, bonitaconnection:true, logger: true, synchronize:true , mapper:true};
	this.status = { properties: {}};
	
	this.truefalse= [ {name:"true", value:"true"}, {name:"false", value:"false"} ];
	this.errorslevel = [ {name:"Warning", value:"WARNING"}, {name:"Info", value:"INFO"}, {name:"Fine", value:"FINE"} ];
	this.lowerupper = [ {name:"lowercase", value:"lowercase"}, {name:"uppercase", value:"uppercase"}, {name:"mixed", value:"mixed"} ];
	
	this.statusldap = {inprogress:""};
	this.statusbonita = { inprogress:""};
	
	this.ldapSynchronizerPath='C:/atelier/LDAP-Synchronizer 6.4.2/BonitaBPMSubscription-6.4.2-LDAP-Synchronizer/conf';
	

	// ------------ init
	this.init=function() {
		this.rolelist= ["member", "admin"];
		var self=this;
		
		$http.get( '?page=custompage_cranetruck&action=initpage&t='+Date.now() )
		.success( function ( jsonResult ) {
				console.log("readProperties",jsonResult);
				self.ldapSynchronizerPath=jsonResult.ldapSynchronizerPath;
				self.domain = jsonResult.domain;
		});
	};
	this.init();
	
		
	// ---------------------------------------------- Properties files
	this.readProperties = function() {
		var post = { "ldapSynchronizerPath" : this.ldapSynchronizerPath, "domain": this.domain };
		var json= encodeURI( angular.toJson(post, false));

		var self=this;
		$http.get( '?page=custompage_cranetruck&action=readfromproperties&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("readProperties",jsonResult);
						
						self.data	= jsonResult;
						self.data.ldap.used_paged_search_list = self.getFromList( self.truefalse, self.data.ldap.used_paged_search);
							
						self.data.logger.log_level_list = self.getLevel( self.data.logger.log_level );
						self.data.sync.error_level_upon_failing_to_get_related_user_list = self.getLevel( self.data.sync.error_level_upon_failing_to_get_related_user_list );
						
						self.data.sync.bonita_username_case_list = self.getFromList( self.lowerupper, self.data.sync.bonita_username_case);
						self.data.sync.bonita_deactivate_users_list = self.getFromList( self.truefalse, self.data.sync.bonita_deactivate_users);
						self.data.sync.allow_recursive_groups_list = self.getFromList( self.truefalse, self.data.sync.allow_recursive_groups);
						
						self.status.properties.status = jsonResult.info;
						self.status.properties.error = jsonResult.error;
						
				})
				.error( function() {
					alert('Error during access the server');
						self.status.properties.error = "Error during access the server";

					});
	}

	this.updateValueFromPage = function() {
			this.data.ldapSynchronizerPath = this.ldapSynchronizerPath;
		if (this.domain != null) {
			this.data.domain = this.domain;
		};
		
		this.data.ldap.used_paged_search = this.data.ldap.used_paged_search_list.value;
		this.data.logger.log_level = this.data.logger.log_level_list.value;
		this.data.sync.bonita_username_case = this.data.sync.bonita_username_case_list.value;
		this.data.sync.error_level_upon_failing_to_get_related_user = this.data.sync.error_level_upon_failing_to_get_related_user_list.value;
		this.data.sync.bonita_deactivate_users = this.data.sync.bonita_deactivate_users_list.value;

	};
	this.writeProperties = function() {
		this.updateValueFromPage();
		var json= encodeURI( angular.toJson(this.data, false));

		var self=this;
		console.log("writeProperties json="+json+" size="+json.length);
		$http.get( '?page=custompage_cranetruck&action=writetoproperties&&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("writeProperties",jsonResult);
							
						self.status.properties.status = jsonResult.info;
						self.status.properties.error = jsonResult.error;
						
				})
				.error( function() {
					alert('Error during access the server');
						self.status.properties.error = "Error during access the server";

					});
	}
	// ---------------------------------------------- LDAP Connection

	this.getDefaultLdapConnection = function() {
		this.data.ldap.hosturl = 'ldap://localhost:10389';
		this.data.ldap.authtype='simple';
		this.data.ldap.principaldn='uid=admin, ou=system';
		this.data.ldap.password='secret';
		this.data.ldap.searchdn='dc=example,dc=com';
		this.data.ldap.searchfilter = 'uid=walter.bates';
		this.data.ldap.directoryusertype='person';
		this.data.ldap.usedpagedsearchlist=this.truefalse[0];
		this.data.ldap.pagesize=1000;
	}
	
	this.ldaptest = {};
	this.testLdapConnection = function()
	{
		this.statusldap.inprogress ="...connection in progress...";		
		var self=this;
		this.updateValueFromPage();
		var json= encodeURI( angular.toJson(this.data.ldap, false));

		$http.get( '?page=custompage_cranetruck&action=testldapconnection&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("result",jsonResult);
						self.statusldap	= jsonResult;
						self.ldaptest			= jsonResult.detailsjsonmap;
						
						self.statusldap.inprogress ="";		
				})
				.error( function() {
					alert('Error while test LDAP connection');
						self.statusldap.inprogress ="";		
					});
				
	};
	// ---------------------------------------------- Bonita Connection
	this.getDefaultBonitaConnection = function()
	{
		var self=this;
		
		$http.get( '?page=custompage_cranetruck&action=getdefaultbonitaconnection&t='+Date.now() )
				.success( function ( jsonResult ) {
					console.log("defaultBonitaConnection",jsonResult);
					self.statusbonita	= jsonResult;
					self.data.bonita = jsonResult;
				})
				.error( function() {
					alert('Error while get default Bonita connection');
					});
				
	};
	
	this.testBonitaConnection = function()
	{
		this.statusbonita.inprogress = "...connection in progress...";		
		
		var self=this;
		var json= encodeURI( angular.toJson(this.data.bonita, false));

		$http.get( '?page=custompage_cranetruck&action=testbonitaconnection&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
					console.log("testBonitaConnection",jsonResult);
					// self.statusbonita.inprogress ="";		
					self.statusbonita	= jsonResult;
				})
				.error( function() {
					// alert('Error while test LDAP connection');
					// self.statusbonita.inprogress ="";		
					self.statusbonita.inprogress ="Error connecting the server";
					});
				
	};
	
	// ---------------------------------------------- getJaasEnvironement
	this.jaasenvironment = {};
	this.jaasenvironment.status = {};

	this.getJaasEnvironment = function() 
	{
		this.jaasenvironment.status.info="In progress";
		this.jaasenvironment.status.error="";
		
		var self=this;
		var json= encodeURI( angular.toJson(this.jaasenvironment.input, false));

		$http.get( '?page=custompage_cranetruck&action=getjaasenvironment&paramjson='+json+'&t='+Date.now() )
				.then( function ( jsonResult ) {
					console.log("return JAAS Environment",jsonResult.data);
					// self.statusbonita.inprogress ="";		
					self.jaasenvironment.status	= jsonResult.data;
				},
				function( jsonResult) {
					alert('Error while test LDAP connection');
					self.jaasenvironment.status.info ="";		
					});
	}
	
	
	// ---------------------------------------------- Synchronize
	this.data.sync.ldap_watched_directories =[];
	this.data.sync.ldap_groups=[]; 
	this.data.sync.ldap_searchs =[];
	this.synctest={};
	
	this.testSynchronize = function() {
		this.updateValueFromPage();
		this.synctest.inprogress = "Test in progress";
		var json= encodeURI( angular.toJson(this.data, false));

		var self=this;
		console.log("writeProperties json="+json+" size="+json.length);
		$http.get( '?page=custompage_cranetruck&action=testsynchronize&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("writeProperties",jsonResult);
							
						self.synctest.inprogress = "";
						self.synctest.status = jsonResult.status;
						self.synctest.error = jsonResult.error;
						self.synctest.detailsjsonmap= json.detailsjsonmap;
						
				})
				.error( function() {
					alert('Error during access the server for Test');
					self.synctest.error = "Error accessing the server.";
					self.synctest.inprogress = "";
					});
	};
	

	// ---------------------------------------------- mapper
	this.data.mapper.listattributes =[];
	this.data.mapper.listattributes.push( { bonitaname:"user_name", "example":"uid" });
	this.data.mapper.listattributes.push( { bonitaname:"first_name", "example":"givenName"});
	this.data.mapper.listattributes.push( { bonitaname:"last_name", "example":"sn"});
	this.data.mapper.listattributes.push( { bonitaname:"title", "example":"title"});
	this.data.mapper.listattributes.push( { bonitaname:"job_title"});
	this.data.mapper.listattributes.push( { bonitaname:"manager"});
	this.data.mapper.listattributes.push( { bonitaname:"delegee"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_email", "example":"mail"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_phone", "example":"telephoneNumber"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_mobile", "example":"mobile"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_fax"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_website"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_room"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_building"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_address", "example":"postalAddress"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_city"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_zip_code", "example":"postalCode"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_state"});
	this.data.mapper.listattributes.push( { bonitaname:"pro_country"});	
	this.data.mapper.listattributes.push( { bonitaname:"perso_email"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_phone"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_mobile"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_fax"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_website"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_room"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_building"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_address"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_city"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_zip_code"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_state"});
	this.data.mapper.listattributes.push( { bonitaname:"perso_country"});
	
	this.mappertest = {};
	
	
	this.testMapper = function() {
		this.mappertest.error = "This function is not yet implemented.";
	};

	// ---------------------------------------------- testAllConfiguration
	this.alltests = {};
	
	this.testAllConfiguration = function() {
		this.alltests.error = "This function is not yet implemented.";
	};
	
	
	
	// ---------------------------------------------- testJaasConnection
	this.jaas= {};
	this.jaas.input={};
	this.jaas.input.inputpassword="password";
	this.jaas.input.showpassword=false;
	this.statusjaas = {};
	this.statusjaas.detailsjsonmap = {};
	this.getDefaultJaasConnection = function()
	{
		this.jaas.input.jaascontent  = 'BonitaAuthentication-1 {\n';
		this.jaas.input.jaascontent  += '    com.sun.security.auth.module.LdapLoginModule REQUIRED\n';
		this.jaas.input.jaascontent  += '    userProvider="ldap://localhost:10389/ou=People,dc=example,dc=com"\n';
		this.jaas.input.jaascontent  += '    userFilter="(&(uid={USERNAME})(objectClass=inetOrgPerson))"\n';
		this.jaas.input.jaascontent  += '    authzIdentity="{USERNAME}"\n';
		this.jaas.input.jaascontent  += '    debug=true\n';
		this.jaas.input.jaascontent  += '    useSSL=false;\n';
		this.jaas.input.jaascontent  += '};';
		this.jaas.input.jaasauthentkey ="BonitaAuthentication-1";
		this.jaas.input.jaasusername ="walter.bates";
		this.jaas.input.jasspassword ="bpm";
	};

	this.showpasswordjaas = function () {
		// the click is call BEFORE the checkbox change
		if (this.jaas.input.showpassword) {
			this.jaas.input.inputpassword="password";
		} else {
			this.jaas.input.inputpassword="text";
		}
	};

	this.testJaasConnection = function()
	{
		this.jaas.inprogress ="...test Jaas in progress...";	
		this.jaas.status	=	"";
		var self=this;
		// var json= angular.toJson(this.jaas.input, false);
		// var jsonurl = json.replace("&","_£");
		var json= encodeURI( angular.toJson(this.jaas.input, false));

		
		$http.get( '?page=custompage_cranetruck&action=testjaasconnection&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("result",jsonResult);
						self.jaas.status			= jsonResult;
						
						self.jaas.inprogress ="";		
				})
				.error( function() {
					alert('Error while test JAAS connection');
					jeself.jaas.inprogress ="";		
					});
				
	};
	
	// ---------------------------------------------- LdapLoginModule
	this.ldaploginmodule= {};
	this.ldaploginmodule.input={};
	this.ldaploginmodule.inprogress="";
	this.ldaploginmodule.status={};
	this.ldaploginmodule.input.showpassword=false;
	this.ldaploginmodule.input.inputpassword="password";

	this.oneevent='<div><button class="btn btn-info" title="com.bonitasoft.custompage.cranetruck.LdapLoginModuleCheck.1">Authentication Id</button><br></div>';
	this.oneevent='I like html<a href="#">link</a>';
	
	this.getDefaultLdaploginmoduleConnection = function() {
		this.ldaploginmodule.input.userProvider="ldap://localhost:10389/ou=People,dc=example,dc=com";
		this.ldaploginmodule.input.userFilter="(&(uid={USERNAME})(objectClass=inetOrgPerson))";
		this.ldaploginmodule.input.authIdentity="{USERNAME}";
		this.ldaploginmodule.input.useSSL=false;
		this.ldaploginmodule.input.login ="walter.bates";
		this.ldaploginmodule.input.password ="bpm";
		this.oneevent='<div><button class="btn btn-info" title="com.bonitasoft.custompage.cranetruck.LdapLoginModuleCheck.1">Authentication Id</button><br></div>';
		
	};
	this.showpasswordldaploginmodule = function () {
		// the click is call BEFORE the checkbox change
		if (this.ldaploginmodule.input.showpassword) {
			this.ldaploginmodule.input.inputpassword="password";
		} else {
			this.ldaploginmodule.input.inputpassword="text";
		}
	};
	
	this.testLdaploginmodule = function() {
		this.ldaploginmodule.inprogress ="...test LdapLoginModule in progress...";		
		var self=this;
		// var json= angular.toJson(this.ldaploginmodule.input, false);
		//var jsonurl = json.replace("&","_£");
		var json= encodeURI( angular.toJson(this.ldaploginmodule.input, false));

		$http.get( '?page=custompage_cranetruck&action=testldaploginmodule&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("result statusldaploginmodule",jsonResult);
						self.ldaploginmodule.status			= jsonResult;
						// self.ldaploginmodule.status.error "Not yet implemented";
						self.ldaploginmodule.inprogress ="";		
				})
				.error( function() {
					alert('Error while test LdapLoginModule connection');
					self.ldaploginmodule.status={};
					self.ldaploginmodule.inprogress ="";		
					});
				
	};
	
	
	// ---------------------------------------------- listusers
	this.listusers= {};
	this.listusers.input = {};
	this.listusers.input.maxtodisplay= 100;
	this.listusers.input.operationselect={};
	this.listusers.input.operationselect.value="DISABLE";
	this.listusers.input.scopeselect={};
	this.listusers.input.scopeselect.value="ALL";
	this.listusers.input.filteruser="";
	this.listusers.totalusers= "unknow";
	this.listusers.list=[];
	this.listusers.operationchoice=[ {name:"Disable (except me)", value:"DISABLE"}, {name:"Enable", value:"ENABLE"},{name:"Delete (except me)", value:"DELETE"} ];
	this.listusers.scopechoice=[ {name:"All", value:"ALL"}, {name:"Use filter (Max number & Filter)", value:"ONLYFIRST"} ];
	this.listusers.input.scopeselect="ONLYFIRST";
	
	this.listusers.status={};
	
	this.listusers.status.info="";
	this.listusers.status.error="statuserror";
	
	
	
	this.getListUsers = function()
	{
		
		this.listusers.status.inprogressgetuser="In progress...";
		
		// var json= angular.toJson(this.listusers.input, false);		
		// var jsonurl = json.replace("&","_£");
		var json= encodeURI( angular.toJson(this.listusers.input, false));

		var self=this;
		
		$http.get( '?page=custompage_cranetruck&action=usersgetlist&paramjson='+json+'&t='+Date.now() )
				.success( function ( jsonResult ) {
						console.log("result statusUsers",jsonResult);
						self.listusers.status			= jsonResult;
						self.listusers.inprogressgetuser="";
						
						self.listusers.list				= jsonResult.detailsjsonmap.list;
						self.listusers.totalusers		= jsonResult.detailsjsonmap.totalusers;

				})
				.error( function() {
					alert('Error while getListUsers connection');
					self.listusers.status.info	="";
					self.listusers.inprogressgetuser="";
					self.listusers.status.error="Error connecting the server";		
					});
	}

	this.listUsersDoOperation = function( action)
	{
		
		if (confirm("Do you really want to "+action+" ? "))
		{
			this.listusers.status.info="In progress...";
			this.listusers.input.operation = action;
			this.listusers.input.scope = this.listusers.input.scopeselect.value;

			var param={'filteruser':this.listusers.input.filteruser, 'operation':action,'scope':this.listusers.input.scope,'maxtodisplay':this.listusers.input.maxtodisplay};
			
			// var json= angular.toJson(this.listusers.input, false);
			// var jsonurl = json.replace("&","_£");
			var json= encodeURI( angular.toJson(param, false));

			var self=this;
			
			$http.get( '?page=custompage_cranetruck&action=usersdooperation&paramjson='+json+'&t='+Date.now() )
					.success( function ( jsonResult ) {
							console.log("result listUsersDoOperation",jsonResult);
							self.listusers.status			= jsonResult;
							self.listusers.list				= jsonResult.detailsjsonmap.list;
							self.listusers.totalusers		= jsonResult.detailsjsonmap.totalusers;
							
					})
					.error( function() {
						alert('Error while userdooperation connection');
						self.listusers.status="";
						self.listusers.error="Error connecting the server";		
						});
		}
	}
	
	
	// ---------------------------------------------- Manage list
	this.list_add = function( listvalues, suffixname, newvalue ) {
		listvalues.push(newvalue);
		this.list_rename( listvalues, suffixname );
	};
	this.list_remove = function( listvalues, nametoremove,suffixname ) {
		console.log("remove: name "+nametoremove+" from list "+listvalues);
		 for (var i=0; i<listvalues.length; i++){
			if (listvalues[ i ].name == nametoremove) {
				console.log("remove: found the correct name at position "+i);
				listvalues.splice( i,1);
			};
         };  
		// rename all		 
		this.list_rename( listvalues, suffixname);
			
	};	
	this.list_rename = function(listvalues, suffixname) {
		// rename all		 
		 for (var i=0; i<listvalues.length; i++){
			listvalues[ i ].name=suffixname+i;
		}
	}
	
	this.getFromList = function( listOfValue, value ) {
		// console.log("getLevel");
		for (var i=0; i<listOfValue.length; i++){
			// console.log("getLevel ["+this.errorslevel[ i ].value+"] to level["+level+"]");
			if (listOfValue.value === value) {
				// console.log("getLevel!! ");
				return listOfValue[ i ];
			}
		}
		return listOfValue[ 0 ];
	}
	
	this.getLevel = function ( level )
	{
		return this.getFromList( this.errorslevel, level );
		/*
		// console.log("getLevel");
		for (var i=0; i<this.errorslevel.length; i++){
			// console.log("getLevel ["+this.errorslevel[ i ].value+"] to level["+level+"]");
			if (this.errorslevel[ i ].value === level) {
				// console.log("getLevel!! ");
				return this.errorslevel[ i ];
			}
		}
		return this.errorslevel[ 0 ];
		*/
	}

	
	this.renderHtml = function(html_code) {
		alert("renderhtml "+html_code);
		// var value=$sce.trustAsHtml(html_code);
		var value=html_code;
		alert("renderhtml "+html_code+">>>"+value);
		return value;
	};
	
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents);
	}
	
});



})();