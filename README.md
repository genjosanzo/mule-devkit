Mule Development Kit
====================

The Mule Development Kit allows the development of Mule Modules using a simple annotation-based system. This kit includes archetypes for cloud connectors, transformers and even transports to accelerate your development. 

Creating a Cloud Connector
--------------------------

Creating a cloud connector using the development kit is extremely easy. Just invoke our archetype as follows:

	mvn archetype:generate -DinteractiveMode=false
	                       -DarchetypeGroupId=org.mule.tools.devkit -DarchetypeArtifactId=mule-devkit-archetype-cloud-connector -DarchetypeVersion=1.0-SNAPSHOT
						   -DgroupId=org.mule.devkit.it -DartifactId=mule-test-module -Dversion=1.0-SNAPSHOT
						   -DmuleVersion=3.1.2 -DmuleModuleName=Test -Dpackage=org.mule.devkit.it
						
Archetype Parameters:

|parameter|description|
|:--------|:----------|
|archetypeGroupId|The group Id of the archetype. This value is ALWAYS org.mule|
|archetypeArtifactId|The artifact Id of the archetype. This value is ALWAYS mule-devkit-archetype-cloud-connector|
|archetypeVersion|The version of the archetype. This value can change as we release new versions of the archetype. Always use the latest non-SNAPSHOT version is possible.|
|groupId|The group Id of the cloud connector you are creating. A good value would be the reserve name of your company domain name, like: com.mulesoft.modules or org.mule.modules|
|artifactId|The artifact Id of the cloud connector you are creating. Please follow the mule naming pattern which is: mule-module-xxx where xxx is the name of your module.|
|version|The version of your cloud connector. Usually 1.0-SNAPSHOT.|
|muleVersion|The version of Mule for which this module is intended. Always use latest non-SNAPSHOT version.|
|muleModuleName|The last word in the artifact Id. If you named youre module "mule-module-xxx" then "xxx" would be the value of this parameter.|
|muleModulePackage|The package of your module. Usually the same as the group Id.|

Creating a Transformer
----------------------

Here is the command line and a brief description on how to create a transformer using our archetype:

Creating a cloud connector using the development kit is extremely easy. Just invoke our archetype as follows:

	mvn archetype:generate -DinteractiveMode=false
	                       -DarchetypeGroupId=org.mule.tools.devkit -DarchetypeArtifactId=mule-devkit-archetype-transformer -DarchetypeVersion=1.0-SNAPSHOT
						   -DgroupId=org.mule.devkit.it -DartifactId=mule-test-module -Dversion=1.0-SNAPSHOT
						   -DmuleVersion=3.1.2 -DmuleModuleName=Test -Dpackage=org.mule.devkit.it
						
Archetype Parameters:

|parameter|description|
|:--------|:----------|
|archetypeGroupId|The group Id of the archetype. This value is ALWAYS org.mule|
|archetypeArtifactId|The artifact Id of the archetype. This value is ALWAYS mule-devkit-archetype-transformer|
|archetypeVersion|The version of the archetype. This value can change as we release new versions of the archetype. Always use the latest non-SNAPSHOT version is possible.|
|groupId|The group Id of the transformer you are creating. A good value would be the reserve name of your company domain name, like: com.mulesoft.modules or org.mule.modules|
|artifactId|The artifact Id of the transformer you are creating. Please follow the mule naming pattern which is: mule-module-xxx where xxx is the name of your module.|
|version|The version of your transformer. Usually 1.0-SNAPSHOT.|
|muleVersion|The version of Mule for which this module is intended. Always use latest non-SNAPSHOT version.|
|muleModuleName|The last word in the artifact Id. If you named youre module "mule-module-xxx" then "xxx" would be the value of this parameter.|
|muleModulePackage|The package of your module. Usually the same as the group Id.|

Creating a Mule Application
--------------------------

Creating a mule application using the mule archetype project is extremely easy. Just invoke it as follows:

mvn archetype:generate -DarchetypeGroupId=org.mule.tools.devkit -DarchetypeArtifactId=mule-devkit-archetype-mule-app \
	-DarchetypeVersion=3.3-SNAPSHOT -DgroupId=org.mule -DartifactId=mule-test-archetype -Dversion=1.0-SNAPSHOT \
	-DmuleVersion=3.2.1 -Dpackage=org.mule -Dtransports=file,http,jdbc,jms,vm -Dmodules=client,cxf,management,scripting,sxc,xml -DstudioNature=true
						
Archetype Parameters:

|parameter|description|default|
|:--------|:----------|:----------|
|archetypeGroupId|The group Id of the archetype This value must ALWAYS org.mule.tools|org.mule.tools|
|archetypeArtifactId|The artifact Id of the archetype| This value must ALWAYS mule-archetype-project|mule-archetype-project|
|archetypeVersion|The version of the archetype. This value can change as we release new versions of the archetype. Always use the latest non-SNAPSHOT version available.|1.5|
|groupId|The group Id of the application you are creating. A good value would be the reserve name of your company domain name, like: com.mulesoft.app or org.mule.app||
|artifactId|The artifact Id of the application you are creating. ||
|version|The version of your application. Usually 1.0-SNAPSHOT.|1.0-SNAPSHOT|
|muleVersion|The version of the mule runtime you are going to use. Mule 2.2.x is no longer supported|3.2.1|
|addAppToClasspath|A flag to either add the src/main/app/ folder as a resource folder to easily access it within your IDE|false|
|transports|A comma separated list of the transport you are going to use within your application.|file,http,jdbc,jms,vm |
|modules|A comma separated list of the modules you are going to use within your application. |client,cxf,management,scripting,sxc,xml |