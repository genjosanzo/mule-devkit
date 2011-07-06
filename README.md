Mule Development Kit
====================

The Mule Development Kit allows the development of Mule Modules using a simple annotation-based system. This kit includes archetypes for cloud connectors, transformers and even transports to accelerate your development. 

Creating a Cloud Connector
--------------------------

Creating a cloud connector using the development kit is extremely easy. Just invoke our archetype as follows:

	mvn archetype:generate -DinteractiveMode=false
	                       -DarchetypeGroupId=org.mule.tools.devkit -DarchetypeArtifactId=mule-devkit-archetype-cloud-connector -DarchetypeVersion=1.0-SNAPSHOT
						   -DgroupId=org.mule.devkit.it -DartifactId=mule-test-module -Dversion=1.0-SNAPSHOT
						   -DmuleVersion=3.1.2 -DmuleModuleName=Test -DmuleModulePackage=org.mule.devkit.it
						
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
						   -DmuleVersion=3.1.2 -DmuleModuleName=Test -DmuleModulePackage=org.mule.devkit.it
						
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