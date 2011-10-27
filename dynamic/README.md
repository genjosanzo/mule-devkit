Mule DevKit Dynamic
===================

Mule DevKit Dynamic is a Java library and also a Mule module that allows the user to dynamically interact with
other Mule models without knowing in advance what those modules are. It also offers reflection capabilities
for knowing what modules support dynamic interaction and with in those what kind of interactions they support.

Creating and configuring a Module
---------------------------------

Let's say you want to configure our Salesforce connector. First of all you need to know what the namespace is of
the module that you are trying to dynamically interact with.

In this case, since we are talking about Salesforce the namespace is:

    http://www.mulesoft.org/schemas/mule/sfdc

So, next we need to call the create method. Remember that we can do so pragmatically or via Mule XML, for the sake of
completion I'm going to show both ways. First, the programmatic version:

    DynamicModule module = new DynamicModule();
    module.setMuleContext(muleContext);
    module.initialise();

    Map<String, Object> attributes = new HashMap<String, Object>();
    attributes.put("username", "john.doe@gmail.com");
    attributes.put("password", "johnd0e");
    attributes.put("securityToken" "abc123");

    module.create("http://www.mulesoft.org/schemas/mule/sfdc", "salesforce-config", attributes);

I guess that last statement requires a little bit of explanation. The first argument of the method is the namespace
of the module that you want to dynamically create and configure. The second argument is the name of the instance. This
argument needs to be unique within your MuleContext (meaning that there cannot be other objects with in the registry
using the same name) and the last argument is the configuration attributes. The attributes that can be configured
varies from module to module.

*The creation process might fail is the proper attributes are not specified.*

Here is the XML equivalent:

    <dynamic:create namespace="http://www.mulesoft.org/schema/mule/sfdc" name="saleforce-config">
        <dynamic:attributes>
            <dynamic:attribute key="username">john.doe@mycompany.com</dynamic:attribute>
            <dynamic:attribute key="password">johnd0e</dynamic:attribute>
            <dynamic:attribute key="securityToken">abc123</dynamic:attribute>
        </dynamic:attributes>
    </dynamic:create>

That last XML element is the equivalent of doing the following:

    <sfdc:config username="john.doe@mycompany.com" password="johnd0e" securityToken="abc123"/>


Destroying a module
-------------------

The module will be automatically destroy once the MuleContext is shutdown, but just in case you want to remove it
from the registry before that we provide a method to remove a dynamically created module.

Here is the Java invocation:

    module.destroy("http://www.mulesoft.org/schemas/mule/sfdc", "salesforce-config");

Also, the XML equivalent:

    <dynamic:destroy namespace="http://www.mulesoft.org/schema/mule/sfdc" name="saleforce-config"/>

