/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mule.devkit.maven;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectHelper;
import org.jfrog.maven.annomojo.annotations.MojoComponent;
import org.jfrog.maven.annomojo.annotations.MojoGoal;
import org.jfrog.maven.annomojo.annotations.MojoParameter;
import org.jfrog.maven.annomojo.annotations.MojoPhase;
import org.jfrog.maven.annomojo.annotations.MojoRequiresDependencyResolution;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.param.Optional;
import org.mule.api.annotations.rest.HttpMethod;
import org.mule.api.annotations.rest.RestCall;
import org.mule.api.annotations.rest.RestHeaderParam;
import org.mule.api.annotations.rest.RestQueryParam;
import org.mule.api.annotations.rest.RestUriParam;
import org.mule.devkit.model.code.AnnotationUse;
import org.mule.devkit.model.code.CatchBlock;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.ClassType;
import org.mule.devkit.model.code.CodeModel;
import org.mule.devkit.model.code.CodeWriter;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@MojoPhase("generate-sources")
@MojoGoal("generate-mashape-client")
@MojoRequiresDependencyResolution("runtime")
public class MashapeGeneratorMojo extends AbstractMuleMojo {
    @MojoComponent
    private MavenProjectHelper projectHelper;

    /**
     * Directory containing the XML definitions for the services
     */
    @MojoParameter(expression = "${project.basedir}/src/main/resources", required = true)
    private File inputFolder;

    @MojoParameter(expression = "${project.build.directory}/generated-sources/mashape", required = true)
    private File defaultOutputDirectory;

    /**
     * Code Model
     */
    private CodeModel codeModel;

    public MashapeGeneratorMojo() {
        codeModel = new CodeModel(new CodeWriter() {
            @Override
            public OutputStream openBinary(org.mule.devkit.model.code.Package pkg, String fileName) throws IOException {
                File file = new File(defaultOutputDirectory, pkg.name().replace(".", "/") + "/" + fileName);
                return FileUtils.openOutputStream(file);
            }

            @Override
            public void close() throws IOException {
            }
        });
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Source directory: " + defaultOutputDirectory.getAbsolutePath() + " added");
        project.addCompileSourceRoot(defaultOutputDirectory.getAbsolutePath());
        try {
            traverse(inputFolder);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void traverse(final File f) throws IOException, MojoExecutionException {
        final File[] childs = f.listFiles();
        for (File child : childs) {
            if (child.isDirectory()) {
                traverse(child);
            } else if (FilenameUtils.isExtension(child.getAbsolutePath(), "mashape")) {
                generateMashapeClient(child);
            }
        }
    }

    private void generateMashapeClient(File inputFile) throws MojoExecutionException {
        getLog().info("Generating POJO for " + inputFile.getAbsolutePath());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        DocumentBuilder db = null;
        try {
            db = factory.newDocumentBuilder();
            Document dom = db.parse(inputFile);

            DefinedClass clazz = codeModel._class(Modifier.ABSTRACT | Modifier.PUBLIC, "org.mule.modules.tinypayme.TinyPayMeConnector", ClassType.CLASS);
            clazz.javadoc().add("Unkown\n\nAutomatically generated from Mashape XML file\n\n@author MuleSoft, Inc.");
            AnnotationUse moduleAnnotation = clazz.annotate(Module.class);
            moduleAnnotation.param("name", "tinypayme");
            moduleAnnotation.param("schemaVersion", "1.0");

            FieldVariable mashapePrivateKey = generateMashapePrivateKey(clazz);
            FieldVariable mashapePublicKey = generateMashapePublicKeyField(clazz);
            FieldVariable mashapeAuthorization = generateMashapeAuthorizationField(clazz);

            generateInit(clazz, mashapePrivateKey, mashapePublicKey, mashapeAuthorization);

            Node api = dom.getElementsByTagName("api").item(0);
            NodeList methods = api.getChildNodes();
            for (int i = 0; i < methods.getLength(); i++) {
                Node method = methods.item(i);

                if (!"method".equals(method.getNodeName())) continue;

                Method restCallMethod = clazz.method(Modifier.PUBLIC | Modifier.ABSTRACT, ref(String.class), method.getAttributes().getNamedItem("name").getNodeValue());
                restCallMethod._throws(ref(IOException.class));
                AnnotationUse messageProcessor = restCallMethod.annotate(Processor.class);
                AnnotationUse restCall = restCallMethod.annotate(RestCall.class);

                Node url = null;
                for (int j = 0; j < method.getChildNodes().getLength(); j++) {
                    if (!"url".equals(method.getChildNodes().item(j).getNodeName())) continue;

                    url = method.getChildNodes().item(j);
                    break;
                }

                String uri = url.getTextContent();
                if (uri.contains("?")) {
                    uri = uri.split("\\?")[0];
                }
                restCall.param("uri", "https://SwnOmB09bsY1a1TfWwxqlAeRo.proxy.mashape.com" + uri);

                if ("GET".equals(method.getAttributes().getNamedItem("http").getNodeValue())) {
                    restCall.param("method", ref(HttpMethod.class).staticRef("GET"));
                } else if ("PUT".equals(method.getAttributes().getNamedItem("http").getNodeValue())) {
                    restCall.param("method", ref(HttpMethod.class).staticRef("PUT"));
                } else if ("POST".equals(method.getAttributes().getNamedItem("http").getNodeValue())) {
                    restCall.param("method", ref(HttpMethod.class).staticRef("POST"));
                }

                Node parameters = null;
                for (int j = 0; j < method.getChildNodes().getLength(); j++) {
                    if (!"parameters".equals(method.getChildNodes().item(j).getNodeName())) continue;

                    parameters = method.getChildNodes().item(j);
                    break;
                }

                for (int j = 0; j < parameters.getChildNodes().getLength(); j++) {
                    if (!"parameter".equals(parameters.getChildNodes().item(j).getNodeName())) continue;

                    Node parameter = parameters.getChildNodes().item(j);

                    Variable restCallParameter = restCallMethod.param(ref(String.class), StringUtils.uncapitalize(camel(parameter.getTextContent())));

                    if (uri.contains("{" + parameter.getTextContent() + "}")) {
                        AnnotationUse restUriParam = restCallParameter.annotate(ref(RestUriParam.class));
                        restUriParam.param("value", parameter.getTextContent());
                    } else {
                        AnnotationUse restQueryParam = restCallParameter.annotate(ref(RestQueryParam.class));
                        restQueryParam.param("value", parameter.getTextContent());
                    }

                    if (parameter.getAttributes().getNamedItem("optional") != null &&
                            "true".equals(parameter.getAttributes().getNamedItem("optional").getNodeValue())) {
                        restCallParameter.annotate(ref(Optional.class));
                    }
                }

            }

            codeModel.build();
        } catch (ParserConfigurationException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        } catch (ClassAlreadyExistsException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private String camel(String uncamelCaseName) {
        String result = "";
        String[] parts = uncamelCaseName.split("_");

        for (int i = 0; i < parts.length; i++) {
            result += StringUtils.capitalize(parts[i].toLowerCase());
        }

        return result;
    }

    private FieldVariable generateMashapePrivateKey(DefinedClass clazz) {
        FieldVariable mashapePrivateKey = clazz.field(Modifier.PRIVATE, ref(String.class), "mashapePrivateKey");
        mashapePrivateKey.javadoc().add("Mashape Private Key");
        mashapePrivateKey.annotate(Configurable.class);

        Method setMashapePrivateKey = clazz.method(Modifier.PUBLIC, codeModel.VOID, "setMashapePrivateKey");
        Variable thatMashapePrivateKey = setMashapePrivateKey.param(ref(String.class), "newMashapePrivateKey");
        setMashapePrivateKey.body().assign(mashapePrivateKey, thatMashapePrivateKey);
        return mashapePrivateKey;
    }

    private FieldVariable generateMashapePublicKeyField(DefinedClass clazz) {
        FieldVariable mashapePublicKey = clazz.field(Modifier.PRIVATE, ref(String.class), "mashapePublicKey");
        mashapePublicKey.javadoc().add("Mashape Public Key");
        mashapePublicKey.annotate(Configurable.class);

        Method setMashapePublicKey = clazz.method(Modifier.PUBLIC, codeModel.VOID, "setMashapePublicKey");
        Variable thatMashapePublicKey = setMashapePublicKey.param(ref(String.class), "newMashapePublicKey");
        setMashapePublicKey.body().assign(mashapePublicKey, thatMashapePublicKey);
        return mashapePublicKey;
    }

    private FieldVariable generateMashapeAuthorizationField(DefinedClass clazz) {
        FieldVariable mashapeAuthorization = clazz.field(Modifier.PRIVATE, ref(String.class), "mashapeAuthorization");
        mashapeAuthorization.javadoc().add("Mashape Authorization");
        AnnotationUse mashapeAuthorizationRestHeaderParam = mashapeAuthorization.annotate(ref(RestHeaderParam.class));
        mashapeAuthorizationRestHeaderParam.param("value", "X-Mashape-Authorization");

        Method getMashapeAuthorization = clazz.method(Modifier.PUBLIC, ref(String.class), "getMashapeAuthorization");
        getMashapeAuthorization.body()._return(mashapeAuthorization);

        return mashapeAuthorization;
    }

    private void generateInit(DefinedClass clazz, FieldVariable mashapePrivateKey, FieldVariable mashapePublicKey, FieldVariable mashapeAuthorization) {
        Method init = clazz.method(Modifier.PUBLIC, codeModel.VOID, "init");
        init.annotate(ref(PostConstruct.class));

        TryStatement attempt = init.body()._try();

        Variable uuid = attempt.body().decl(ref(String.class), "uuid", ref(UUID.class).staticInvoke("randomUUID").invoke("toString"));
        Variable signingKey = attempt.body().decl(ref(SecretKeySpec.class), "signingKey", ExpressionFactory._new(ref(SecretKeySpec.class)).arg(mashapePrivateKey.invoke("getBytes")).arg("HmacSHA1"));
        Variable mac = attempt.body().decl(ref(Mac.class), "mac", ref(Mac.class).staticInvoke("getInstance").arg("HmacSHA1"));
        attempt.body().add(mac.invoke("init").arg(signingKey));
        Variable rawHmac = attempt.body().decl(codeModel.BYTE.array(), "rawHmac", mac.invoke("doFinal").arg(uuid.invoke("getBytes")));
        Variable hex = attempt.body().decl(codeModel.BYTE.array(), "hex", ExpressionFactory._new(ref(Hex.class)).invoke("encode").arg(rawHmac));
        Variable headerValue = attempt.body().decl(ref(String.class), "headerValue", Op.plus(Op.plus(Op.plus(mashapePublicKey, ExpressionFactory.lit(":")), ExpressionFactory._new(ref(String.class)).arg(hex).arg("UTF-8")), uuid));

        Invocation getHeaderValueBytes = ref(Base64.class).staticInvoke("encodeBase64String").arg(headerValue.invoke("getBytes"));
        attempt.body().assign(mashapeAuthorization, getHeaderValueBytes.invoke("replace").arg("\r\n").arg(""));

        CatchBlock catchUnsupportedEncodingExpception = attempt._catch(ref(UnsupportedEncodingException.class));
        CatchBlock catchNoSuchAlgorithmException = attempt._catch(ref(NoSuchAlgorithmException.class));
        CatchBlock catchInvalidKeyException = attempt._catch(ref(InvalidKeyException.class));
    }

    private TypeReference ref(Class<?> clazz) {
        return codeModel.ref(clazz);
    }

}